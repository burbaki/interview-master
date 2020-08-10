package forex.cache.rates

import cats.Applicative
import forex.domain.Rate

trait StorageAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[Either[errors.StorageError, Rate]]
  def put(pair: Rate.Pair, rate: Rate): F[Either[errors.StorageError, Rate]]
  def cleanUp: F[Unit]
}

object StorageAlgebra {
  def inMemoryStorage[F[_]: Applicative] = new InMemoryStorage[F]
}
