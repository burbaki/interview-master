package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.config.ClientConfig
import forex.services.rates.interpreters._
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client

object Interpreters {
  def oneFrame[F[_]: Applicative: Sync: Logger](client: Client[F], config: ClientConfig): ServiceAlgebra[F] =
    new OneFrame[F](client, config)
}
