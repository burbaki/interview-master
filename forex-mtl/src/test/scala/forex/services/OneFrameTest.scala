package forex.services

import java.time.OffsetDateTime

import cats.effect._
import forex.TestUtils
import forex.config.ClientConfig
import forex.domain.{Currency, Price, Rate}
import forex.http._
import forex.services.rates.errors.Error.{ClientResponseParseError, LookupFailed, WrongUri}
import forex.services.rates.interpreters.OneFrame._
import forex.services.rates.{Interpreters, errors}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s._
import org.http4s.client.Client
import org.http4s.headers.Accept
import org.http4s.util.CaseInsensitiveString
import org.scalatest.GivenWhenThen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class OneFrameTest extends AnyFunSuite with Matchers with GivenWhenThen {

  implicit val config: ClientConfig = ClientConfig("test", "testhost", 1111, List("testtoken"))
  implicit val logger: Logger[IO]   = Slf4jLogger.getLogger

  def target(response: Response[IO], expectedRequest: Request[IO])(implicit config: ClientConfig) =
    Interpreters.oneFrame[IO](TestUtils.client(response, Some(expectedRequest)), config)

  test("OneFrame interpreter should create correct request") {
    val pair         = Rate.Pair(Currency.fromString("USD"), Currency.fromString("EUR"))
    val price: Price = Price(0.23)
    val rawResponse =
      OneFrameResponse(pair.from, pair.to, Random.nextDouble(), Random.nextDouble(), price, OffsetDateTime.now)
    val response =
      Response[IO](status = Status.Ok).withEntity(List(rawResponse))

    val expectedRequest: Request[IO] = Request(
      uri = uri"http://testhost:1111/rates?pair=USDEUR",
      headers = Headers.of(Header.apply("Token", config.tokens.head), Accept(MediaType.application.json))
    )

    val result: Either[errors.ServiceError, Rate] = target(response, expectedRequest).get(pair).unsafeRunSync()

    result shouldEqual Right(Rate(pair, price, rawResponse.timestamp))
  }

  test("OneFrame interpreter should deal with wrong response") {
    val pair = Rate.Pair(Currency.fromString("USD"), Currency.fromString("EUR"))

    val response =
      Response[IO](status = Status.Ok).withEntity("It was me, wrong response!")

    val expectedRequest: Request[IO] = Request(
      uri = uri"http://testhost:1111/rates?pair=USDEUR",
      headers = Headers.of(Header.apply("Token", config.tokens.head), Accept(MediaType.application.json))
    )

    val result: Either[errors.ServiceError, Rate] = target(response, expectedRequest).get(pair).unsafeRunSync()
    result shouldEqual Left(ClientResponseParseError("Malformed message body: Invalid JSON"))
  }

  test("OneFrame interpreter should deal with error response") {
    val pair = Rate.Pair(Currency.fromString("USD"), Currency.fromString("EUR"))

    val response =
      Response[IO](status = Status.BadRequest).withEntity(OneFrameError("An error"))

    val expectedRequest: Request[IO] = Request(
      uri = uri"http://testhost:1111/rates?pair=USDEUR",
      headers = Headers.of(Header.apply("Token", config.tokens.head), Accept(MediaType.application.json))
    )

    val result: Either[errors.ServiceError, Rate] = target(response, expectedRequest).get(pair).unsafeRunSync()
    result shouldEqual Left(LookupFailed("Request to OneFrame was failed"))
  }

  test("OneFrame interpreter should deal with wrong uri") {
    val pair = Rate.Pair(Currency.fromString("USD"), Currency.fromString("EUR"))

    val response =
      Response[IO](status = Status.Ok).withEntity("It was me, wrong response!")

    val expectedRequest: Request[IO] = Request(
      uri = uri"http://testhost:1111/rates?pair=USDEUR",
      headers = Headers.of(Header.apply("Token", config.tokens.head), Accept(MediaType.application.json))
    )

    val result: Either[errors.ServiceError, Rate] =
      target(response, expectedRequest)(config.copy(host = "]]]]")).get(pair).unsafeRunSync()
    result shouldEqual Left(WrongUri("Invalid URI"))
  }

  test("ensuring that changing token works correct") {
    Given("quota reach token, fresh token, config, client stub")
    val pair         = Rate.Pair(Currency.fromString("USD"), Currency.fromString("EUR"))
    val price: Price = Price(0.23)
    val token        = "token1"
    val freshToken   = "token2"
    val config       = ClientConfig("test", "testhost", 1111, List(token, freshToken))
    val rawResponse =
      OneFrameResponse(pair.from, pair.to, Random.nextDouble(), Random.nextDouble(), price, OffsetDateTime.now)

    val client: Client[IO] = Client[IO](req => {
      val resp = req.headers.get(CaseInsensitiveString("token")) match {
        case Some(Header(_, "token1")) =>
          Response[IO](status = Status.Forbidden).withEntity(OneFrameError("Quota reached"))
        case Some(Header(_, "token2")) => Response[IO](status = Status.Ok).withEntity(List(rawResponse))
      }
      Resource.apply[IO, Response[IO]](IO(resp -> IO(())))
    })
    val target = Interpreters.oneFrame[IO](client, config)

    When("get rate from target")
    val response: Either[errors.ServiceError, Rate] = target.get(pair).unsafeRunSync()

    Then("response should contain correct price")

    response shouldEqual Right(Rate(pair, price, rawResponse.timestamp))

  }
}
