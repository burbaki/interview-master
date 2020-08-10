package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import forex.http.rates.Protocol.GetApiResponse._
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import io.circe.Encoder
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"
  implicitly[Encoder[GetApiResponse]]
  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
        case Right(rate) => Ok(rate.asGetApiResponse)
        case Left(error) =>
          error match {
            case Error.RateLookupFailed(errorType, msg) => InternalServerError(GetErrorResponse(msg, errorType))
            case Error.ValidationError(field, msg) =>
              BadRequest(GetErrorResponse(msg, s"field validation for: $field"))
          }

      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
