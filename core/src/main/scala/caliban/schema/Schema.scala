package caliban.schema

trait Schema[-R, T] { self =>

  private lazy val asType: __Type             = toType()
  private lazy val asInputType: __Type        = toType(isInput = true)
  private lazy val asSubscriptionType: __Type = toType(isSubscription = true)

  final def toType_(isInput: Boolean = false, isSubscription: Boolean = false): __Type =
    if (isInput) asInputType else if (isSubscription) asSubscriptionType else asType

  protected[this] def toType(isInput: Boolean = false, isSubscription: Boolean = false): __Type

  def resolve(value: T): Step[R]

  def optional: Boolean = false

  def arguments: List[__InputValue] = Nil

  def contramap[A](f: A => T): Schema[R, A] = new Schema[R, A] {
    override def optional: Boolean = self.optional
    override def arguments: List[__InputValue] = self.arguments
    override def toType(isInput: Boolean, isSubscription: Boolean): __Type = self.toType(isInput, isSubscription)
    override def resolve(value: A): Step[R] = self.resolve(f(value))
  }
}
