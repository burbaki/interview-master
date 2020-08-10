package forex.config

import forex.domain.Currency

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(http: HttpConfig,
                             clients: List[ClientConfig],
                             currencyTtl: FiniteDuration,
                             currencyWhitelist: List[Currency])

case class ClientConfig(name: String, host: String, port: Int, tokens: List[String])

case class HttpConfig(host: String, port: Int, timeout: FiniteDuration)
