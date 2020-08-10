package forex.domain

import io.circe.generic.extras.semiauto.{deriveUnwrappedDecoder, deriveUnwrappedEncoder}
import io.circe.{Decoder, Encoder}

case class Price(value: BigDecimal) extends AnyVal

object Price {

  implicit val encoder: Encoder[Price] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[Price] = deriveUnwrappedDecoder

  def apply(value: Integer): Price =
    Price(BigDecimal(value))
}
