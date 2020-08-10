package forex.http
package rates

import java.time.OffsetDateTime

import forex.domain._
import io.circe.generic.JsonCodec
object Protocol {

  @JsonCodec
  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )
  @JsonCodec
  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: OffsetDateTime
  )

  @JsonCodec
  final case class GetErrorResponse(msg: String, errorType: String)
}
