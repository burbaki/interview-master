package forex.cache.rates

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.cache.rates.errors.NotFoundPair
import forex.domain.Rate

import scala.collection.concurrent.Map
class InMemoryStorage[F[_]: Applicative] extends StorageAlgebra[F] {

  val storage: Map[Rate.Pair, Rate] = scala.collection.concurrent.TrieMap.empty

  override def get(pair: Rate.Pair): F[Either[errors.StorageError, Rate]] =
    storage.get(pair).toRight(NotFoundPair(pair.toString): errors.StorageError).pure[F]

  override def put(pair: Rate.Pair, rate: Rate): F[Either[errors.StorageError, Rate]] =
    storage.put(pair, rate).getOrElse(rate).asRight[errors.StorageError].pure[F]

  override def cleanUp: F[Unit] = storage.clear().pure[F]
}
