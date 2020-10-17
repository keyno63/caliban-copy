package caliban

trait GraphQL[-R] { self =>

  protected val schemaBuilder: RootSchemaBuilder[R]
  protected val wrappers: List[Wrapper[R]]
  protected val additionalDirectives: List[__Directive]

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

    val wrappers: List[Wrapper[R]] = Nil
    val additionalDirectives: List[__Directive] = directives
  }
}