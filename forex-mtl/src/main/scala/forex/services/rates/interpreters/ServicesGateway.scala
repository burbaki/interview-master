package forex.services.rates.interpreters

import java.time.OffsetDateTime

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import forex.cache.rates.StorageAlgebra
import forex.cache.rates.errors.NotFoundPair
import forex.domain.Rate
import forex.services.rates.errors.ServiceError
import forex.services.rates.{ServiceAlgebra, errors}
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration.Duration

class ServicesGateway[F[_]: Monad: Logger](serviceInstances: List[ServiceAlgebra[F]],
                                           storage: StorageAlgebra[F],
                                           ttl: Duration)
    extends ServiceAlgebra[F] {

  val serviceInstance: ServiceAlgebra[F] = serviceInstances.head

  override def get(pair: Rate.Pair): F[Either[ServiceError, Rate]] =
    storage.get(pair).flatMap {
      case Left(NotFoundPair(_)) =>
        Logger[F].info(s"Not found value in storage for $pair") *> refreshPair(pair)

      case Right(rate) if isOverdue(rate.timestamp) =>
        Logger[F].info(s"Value in storage $pair was overdue") *> refreshPair(pair)
      case Right(rate) => Logger[F].info(s"Use value from storage $pair") *> rate.asRight[ServiceError].pure[F]
    }

  private def refreshPair(pair: Rate.Pair): F[Either[ServiceError, Rate]] =
    for {
      rate <- (for {
               serviceResult <- EitherT(serviceInstance.get(pair))
               _ <- EitherT(storage.put(pair, serviceResult)).leftMap(errors.Error.toServiceError(_): ServiceError)
             } yield serviceResult).value
      _ <- Logger[F].info(s"Refreshed: $pair -> $rate")
    } yield rate

  private def isOverdue(timestamp: OffsetDateTime): Boolean =
    OffsetDateTime.now.minusMinutes(ttl.toMinutes).isAfter(timestamp)

}

object ServicesGateway {
  def apply[F[_]: Monad: Logger](serviceInstances: List[ServiceAlgebra[F]], storage: StorageAlgebra[F], ttl: Duration) =
    new ServicesGateway[F](serviceInstances, storage, ttl)
}
