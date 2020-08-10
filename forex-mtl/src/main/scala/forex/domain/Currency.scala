package forex.domain

import cats.Show
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto._

case class Currency(value: String) extends AnyVal

object Currency {

  implicit val encoder: Encoder[Currency] = deriveUnwrappedEncoder
  implicit val decoder: Decoder[Currency] = deriveUnwrappedDecoder
  implicit val show: Show[Currency] = Show.show {
    _.value
  }

  def fromString(s: String): Currency =
    Currency(s.toUpperCase)

}
