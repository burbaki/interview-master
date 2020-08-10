package forex.domain
import java.time.OffsetDateTime

import io.circe.generic.JsonCodec

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: OffsetDateTime
)

object Rate {
  @JsonCodec
  final case class Pair(
      from: Currency,
      to: Currency
  )
}
