package caliban.interop.play

import play.api.libs.json.{ JsValue, Json, JsonValidationError, Reads, Writes }
import zio.query.ZQuery

private[caliban] trait IsPlayJsonWrites[F[_]]
private[caliban] object IsPlayJsonWrites {
  implicit val isPlayJsonWrites: IsPlayJsonWrites[Writes] = null
}

private[caliban] trait IsPlayJsonReads[F[_]]
private[caliban] object IsPlayJsonReads {
  implicit val isPlayJsonReads: IsPlayJsonReads[Reads] = null
}

object json {
  implicit val jsonSchema: Schema[Any, JsValue] = new Schema[Any, JsValue] {
    private def parse(value: JsValue) =
      implicitly[Reads[ResponseValue]]
        .reads(value)
        .asEither
        .left
        .map(parsingException)

    override def toType(isInput: Boolean, isSubscription: Boolean): __Type = makeScalar("Json")
    override def resolve(value: JsValue): Step[Any] =
      QueryStep(ZQuery.fromEffect(ZIO.fromEither(parse(value))).map(PureStep))
  }
  implicit val jsonArgBuilder: ArgBuilder[JsValue] = (input: InputValue) => Right(Json.toJson(input))

  private[caliban] def parsingException(
    errs: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]
  ) =
    new Throwable(s"Couldn't decode json: $errs")
}
