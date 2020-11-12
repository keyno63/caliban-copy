package caliban.schema

import java.util.UUID

import zio.Chunk

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
    override def optional: Boolean                                         = self.optional
    override def arguments: List[__InputValue]                             = self.arguments
    override def toType(isInput: Boolean, isSubscription: Boolean): __Type = self.toType(isInput, isSubscription)
    override def resolve(value: A): Step[R]                                = self.resolve(f(value))
  }
}

object Schema extends GenericSchema[Any]

trait GenericSchema[R] extends DerivationSchema[R] with TemporalSchema {

  def scalarSchema[A](name: String, description: Option[String], makeResponse: A => ResponseValue): Schema[Any, A] =
    new Schema[Any, A] {
      override def toType(isInput: Boolean, isSubscription: Boolean): __Type = makeScalar(name, description)
      override def resolve(value: A): Step[Any]                              = pureStep(makeResponse(value))
    }

  def objectSchema[R1, A](
    name: String,
    description: Option[String],
    fields: (Boolean, Boolean) => List[(__Field, A => Step[R1])],
    directives: List[Directive] = List.empty
  ): Schema[R1, A] =
    new Schema[R1, A] {

      override def toType(isInput: Boolean, isSubscription: Boolean): __Type =
        if (isInput) {
          makeInputObject(Some(customizeInputTypeName(name)), description, fields(isInput, isSubscription).map {
            case (f, _) => __InputValue(f.name, f.description, f.`type`, None)
          })
        } else makeObject(Some(name), description, fields(isInput, isSubscription).map(_._1), directives)

      override def resolve(value: A): Step[R1] =
        ObjectStep(name, fields(false, false).map { case (f, plan) => f.name -> plan(value) }.toMap)
    }

  implicit val unitSchema: Schema[Any, Unit]             = scalarSchema("unit", None, _ => ObjectValue(Nil))
  implicit val booleanSchema: Schema[Any, Boolean]       = scalarSchema("Boolean", None, BooleanValue)
  implicit val stringScheam: Schema[Any, String]         = scalarSchema("String", None, StringValue)
  implicit val uuidSchema: Schema[Any, UUID]             = scalarSchema("ID", None, uuid => StringValue(uuid.toString))
  implicit val intSchema: Schema[Any, Int]               = scalarSchema("Int", None, IntValue(_))
  implicit val longSchema: Schema[Any, Long]             = scalarSchema("Long", None, IntValue(_))
  implicit val bigIntSchema: Schema[Any, BigInt]         = scalarSchema("BigInt", None, IntValue(_))
  implicit val doubleSchema: Schema[Any, Double]         = scalarSchema("Double", None, FloatValue(_))
  implicit val floatSchema: Schema[Any, Float]           = scalarSchema("Float", None, FloatValue(_))
  implicit val bigDecimalSchema: Schema[Any, BigDecimal] = scalarSchema("BigDecimal", None, FloatValue(_))

  implicit def optionSchema[A](implicit ev: Schema[R, A]): Schema[R, Option[A]] = new Schema[R, Option[A]] {
    override def optional: Boolean                                                 = ???
    override protected[this] def toType(isInput: Boolean, isSubscription: Boolean) = ???
    override def resolve(value: Option[A])                                         = ???
  }

  implicit def listSchema[A](implicit ev: Schema[R, A]): Schema[R, List[A]] = new Schema[R, List[A]] {
    override protected[this] def toType(isInput: Boolean, isSubscription: Boolean) = ???
    override def resolve(value: List[A])                                           = ???
  }

  implicit def setSchema[A](implicit ev: Schema[R, A]): Schema[R, Set[A]]       = listSchema[A].contramap(_.toList)
  implicit def seqSchema[A](implicit ev: Schema[R, A]): Schema[R, Seq[A]]       = listSchema[A].contramap(_.toList)
  implicit def vectorSchema[A](implicit ev: Schema[R, A]): Schema[R, Vector[A]] = listSchema[A].contramap(_.toList)
  implicit def chunkSchema[A](implicit ev: Schema[R, A]): Schema[R, Chunk[A]]   = listSchema[A].contramap(_.toList)
  implicit def functionUnitSchema[A](implicit ev: Schema[R, A]): Schema[R, () => A] =
    new Schema[R, () => A] {
      override def optional: Boolean                                                 = ???
      override protected[this] def toType(isInput: Boolean, isSubscription: Boolean) = ???
      override def resolve(value: () => A)                                           = ???
    }

  implicit def eitherSchema[RA, RB, A, B](
    implicit ev: Schema[RA, A],
    evB: Schema[RB, B]
  ): Schema[RA with RB, Either[A, B]] =
    ???
}