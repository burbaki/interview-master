package forex.services.rates.interpreters

import java.time.OffsetDateTime

import cats.Applicative
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import forex.config.ClientConfig
import forex.domain.{ Currency, Price, Rate }
import forex.http._
import forex.services.rates.ServiceAlgebra
import forex.services.rates.errors.Error.{ ClientResponseParseError, LookupFailed, WrongUri }
import forex.services.rates.errors._
import forex.services.rates.interpreters.OneFrame._
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.JsonCodec
import io.circe.generic.extras.{ ConfiguredJsonCodec, JsonKey }
import io.circe.generic.extras.defaults.defaultGenericConfiguration
import org.http4s.Status.Successful
import org.http4s._
import org.http4s.client._
import org.http4s.headers.Accept

class OneFrame[F[_]: Applicative: Sync: Logger](client: Client[F], config: ClientConfig) extends ServiceAlgebra[F] {

  private val clientPrefix = s"http://${config.host}:${config.port}/rates"

  // Not sure, but will be better if we pass this like constructor argument, and create F[Ref[_]} in Main.
  // But Main shouldn't create some state for service
  private val tokensRef: Ref[F, List[TokenState]] = Ref.unsafe(config.tokens.map(t => TokenState(t, None)))

  private def chooseToken: F[List[String]] =
    for {
      // With this we sync our unsafe ref with our IO context. This prevents a sending requests until ref will be updated.
      // Test logs proves It
      syncRef <- implicitly[Sync[F]].delay(tokensRef)
      refreshedTokens <- syncRef.updateAndGet(
                          l =>
                            l.map {
                              case t if t.refreshTime.exists(OffsetDateTime.now.isAfter) => t.copy(refreshTime = None)
                              case t                                                     => t
                          }
                        )
      availableTokens <- refreshedTokens.filter(_.refreshTime.isEmpty).map(_.token).pure[F]
    } yield availableTokens

  private def setRefreshTime(targetToken: String): F[Unit] =
    for {
      syncRef <- implicitly[Sync[F]].delay(tokensRef)
      _ <- syncRef.update(
            list =>
              list.map {
                case t if t.token == targetToken => t.copy(refreshTime = Some(OffsetDateTime.now))
                case t                           => t
            }
          )
      _ <- Logger[F].info(s"token $targetToken will be available tomorrow")
    } yield ()

  override def get(pair: Rate.Pair): F[Either[ServiceError, Rate]] =
    Uri.fromString(s"$clientPrefix${pair.asQueryParams}") match {
      case Right(uri) =>
        for {
          tokens <- chooseToken
          request <- Request[F](
                      uri = uri,
                      headers = Headers.of(Header.apply("Token", tokens.head), Accept(MediaType.application.json))
                    ).pure[F]
          _ <- Logger[F].info(s"Send request to ${clientPrefix} with $pair")

          rate <- client.fetch(request) {
                   case Successful(resp) =>
                     jsonDecoder[List[OneFrameResponse], F]
                       .decode(resp, false)
                       .map(_.head.asRate)
                       .leftMap(decodeFailure => ClientResponseParseError(decodeFailure.message): ServiceError)
                       .value
                   case failedResp =>
                     for {
                       error <- jsonDecoder[OneFrameError, F].decode(failedResp, false).value
                       _ <- Logger[F].info(s"Got service response with status: ${failedResp.status} and body: $error")
                       result <- error match {
                                  case Right(OneFrameError("Quota reached")) =>
                                    setRefreshTime(tokens.head) *> this.get(pair)
                                  case _ =>
                                    (LookupFailed("Request to OneFrame was failed"): ServiceError).asLeft[Rate].pure[F]
                                }
                     } yield result
                 }
        } yield rate

      // TODO: will be nice to create util function
      //  for handling 200 response which applies implicit decoder and partial function for handling non 200 response
      case Left(ex) =>
        Logger[F].error(ex)("Wrong Uri") *>
          (WrongUri(ex.sanitized): ServiceError).asLeft[Rate].pure[F]
    }
}

object OneFrame {

  case class TokenState(token: String, refreshTime: Option[OffsetDateTime])

  implicit class GetParameters(val pair: Rate.Pair) {
    def asQueryParams: String =
      s"?pair=${pair.from.value}${pair.to.value}"
  }

  implicit class GetRate(val resp: OneFrameResponse) {
    def asRate: Rate =
      Rate(Rate.Pair(resp.from, resp.to), resp.price, resp.timestamp)
  }

  @ConfiguredJsonCodec
  final case class OneFrameResponse(from: Currency,
                                    to: Currency,
                                    bid: BigDecimal,
                                    ask: BigDecimal,
                                    price: Price,
                                    @JsonKey("time_stamp") timestamp: OffsetDateTime)

  @JsonCodec
  final case class OneFrameError(error: String)

}
