package caliban

import java.beans.Introspector

trait GraphQL[-R] { self =>

  protected val schemaBuilder: RootSchemaBuilder[R]
  protected val wrappers: List[Wrapper[R]]
  protected val additionalDirectives: List[__Directive]

  final def render: String =
    s"""schema {
       |${schemaBuilder.query.flatMap(_.opType.name).fold("")(n => s" query: $n\n")}${schemaBuilder.mutation
         .flatMap(_.opType.name)
         .fold("")(n => s" mution: $n\n")}${schemaBuilder.subscription
         .flatMap(_.opType.name)
         .fold("")(n => s" subscription: $n\n")}}
       |
       |${renderTypes(schemaBuilder.types)}""".stripMargin

  final def toDucument: Document =
    Document(
      SchemaDefinition(
        Nil,
        schemaBuilder.query.flatMap(_.opType.name),
        schemaBuilder.mutation.flatMap(_.opType.name),
        schemaBuilder.subscription.flatMap(_.opType.name)
      ) :: schemaBuilder.types.flatMap(_.toTypeDefinition) ++ additionalDirectives.map(_.toDirectiveDefinition),
      SourceMapper.empty
    )

  final def interpreter: IO[ValidationError, GraphQLInterpreter[R, CalibanError]] =
    Validator
      .validateSchema(schemaBuilder)
      .map { schema =>
        lazy val rootType =
          RootType(
            schema.query.opType,
            schema.mutation.map(_.opType),
            schema.subscription.map(_.opType),
            additionalDirectives
          )
        lazy val introspectionRootSchema: RootSchema[Any] = Introspector.introspect(rootType)
        lazy val introspectionRootType: RootType          = RootType(introspectionRootSchema.query.opType, None, None)

        new GraphQLInterpreter[R, CalibanError] {
          override def check(query: String): IO[CalibanError, Unit] =
            for {
              document       <- Parser.parseQuery(query)
              intro          = Introspector.isIntrospection(document)
              typeToValidate = if (intro) introspectionRootType else rootType
              _              <- Validator.validate(document, typeToValidate)
            } yield ()

          override def executeRequest(
            request: GraphQLRequest,
            skipValidation: Boolean,
            enableIntrospection: Boolean
          ): URIO[R, GraphQLResponse[CalibanError]] = OperationType
        }
      }

  final def withWrapper[R2 <: R](wrapper: Wrapper[R2]): GraphQL[R2] =
    new GraphQL[R2] {
      override protected val schemaBuilder: RootSchemaBuilder[R2]    = self.schemaBuilder
      override protected val wrappers: List[Wrapper[R2]]             = wrapper :: self.wrappers
      override protected val additionalDirectives: List[__Directice] = self.additionalDirectives
    }

  final def @@[R2 <: R](wrapper: Wrapper[R2]): GraphQL[R2] = withWrapper(wrapper)

  final def combine[R1 <: R](that: GraphQL[R1]): GraphQL[R1] =
    new GraphQL[R1] {
      override protected val schemaBuilder: RootSchemaBuilder[R1] = self.schemaBuilder |+| that.schemaBuilder
      override protected val wrappers: List[Wrapper[R1]]          = self.wrappers ++ that.wrappers
      override protected val additionalDirectives: List[__Directive] =
        self.additionalDirectives ++ that.additionalDirectives
    }

  final def |+|[R1 <: R](that: GraphQL[R1]): GraphQL[R1] = combine(that)

  final def rename(
    queriesName: Option[String] = None,
    mutationsName: Option[String] = None,
    subscriptionName: Option[String] = None
  ): GraphQL[R] = new GraphQL[R] {
    override protected val schemaBuilder: RootSchemaBuilder[R]     = ???
    override protected val wrappers: List[Wrapper[R]]              = self.wrappers
    override protected val additionalDirectives: List[__Directive] = self.additionalDirectives
  }
}

object GraphQL {
  def graphQL[R, Q, M, S: SubscriptionSchema](resolver: RootResolver[Q, M, S], directives: List[__Directive] = Nil)(
    implicit querySchema: Schema[R, Q],
    mutationSchema: Schema[R, M],
    subscriptionSchema: Schema[R, S]
  ): GraphQL[R] = new GraphQL[R] {
    val schemaBuilder: RootSchemaBuilder[R] = RootSchemaBuilder(
      resolver.queryResolver.map(r => Operation(querySchema.toType_(), querySchema.resolve(r))),
      resolver.mutationResolver.map(r => Operation(mutationSchema.toType_(), mutationSchema.resolve(r))),
      resolver.subscriptionResolver.map(r =>
        Operaion(subscriptionSchema.toType_(isSubscription = true), subscriptionSchema.resolve(r))
      )
    )

    val wrappers: List[Wrapper[R]]              = Nil
    val additionalDirectives: List[__Directive] = directives
  }
}
