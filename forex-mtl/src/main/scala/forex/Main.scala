package forex

import cats.effect._
import forex.config._
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream.compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  implicit val logger = Slf4jLogger.getLogger

  def stream: Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      client <- Stream.resource(
                 BlazeClientBuilder[F](global)
                   .withRequestTimeout(config.http.timeout)
                   .resource
               )

      module = new Module[F](client, config)
      _ <- BlazeServerBuilder[F]
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()
}
