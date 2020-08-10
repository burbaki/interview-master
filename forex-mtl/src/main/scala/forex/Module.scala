package forex

import cats.effect.{Concurrent, Timer}
import forex.cache.rates.{InMemoryStorage, StorageAlgebra}
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import forex.services.rates.interpreters.ServicesGateway
import forex.validation.rates.ValidationAlgebra
import forex.validation.simpleValidation
import io.chrisdavenport.log4cats.Logger
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}

class Module[F[_]: Concurrent: Timer: Logger](client: Client[F], config: ApplicationConfig) {

  private val ratesService =
    config.clients.find(_.name == "one-frame").map(conf => RatesServices.oneFrame(client, conf))

  private val storage: InMemoryStorage[F] = StorageAlgebra.inMemoryStorage

  private val servicesGateway: RatesService[F] =
    ServicesGateway(List(ratesService).flatten, storage, config.currencyTtl)

  private val validation: ValidationAlgebra = simpleValidation(config.currencyWhitelist)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](servicesGateway, validation)

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
