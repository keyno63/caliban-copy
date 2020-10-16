package caliban

trait GraphQL[-R] {
  self =>

}

object GraphQL {
  def graphQL[R, Q, M, S: SubscriptionSchema](resolver: RootResolver[Q, M, S], directives: List[__Directive] = Nil)(
        implicit querySchema: Schema[R, Q],
             mutationSchema: Schema[R, M],
        subscriptionSchema: Schema[R, S]
  ): GraphQL[R] = new GraphQL[R] {

  }
}