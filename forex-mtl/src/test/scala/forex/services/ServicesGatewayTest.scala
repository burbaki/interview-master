package forex.services;

import java.time.OffsetDateTime

import cats.effect.IO
import forex.TestUtils
import forex.cache.rates.{InMemoryStorage, StorageAlgebra}
import forex.config.ClientConfig
import forex.domain.{Currency, Price, Rate}
import forex.http._
import forex.services.rates.Interpreters
import forex.services.rates.interpreters.OneFrame.OneFrameResponse
import forex.services.rates.interpreters.ServicesGateway
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.headers.Accept
import org.http4s.{Headers, MediaType, Request, Response, Status, _}
import org.scalatest.GivenWhenThen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.util.Random
class ServicesGatewayTest extends AnyFunSuite with Matchers with GivenWhenThen {

  val ttl: FiniteDuration     = 5.minutes
  val now: OffsetDateTime     = OffsetDateTime.now
  val pair: Rate.Pair         = Rate.Pair(Currency.fromString("USD"), Currency.fromString("EUR"))
  val priceFromService: Price = Price(0.23)
  val priceFromStorage: Price = Price(0.33)

  val rawResponse =
    OneFrameResponse(pair.from, pair.to, Random.nextDouble(), Random.nextDouble(), priceFromService, now)
  val response =
    Response[IO](status = Status.Ok).withEntity(List(rawResponse))

  implicit val config: ClientConfig = ClientConfig("test", "testhost", 1111, List("testtoken"))
  implicit val logger: Logger[IO]   = Slf4jLogger.getLogger

  def oneFrameService(response: Response[IO], expectedRequest: Option[Request[IO]])(implicit config: ClientConfig) =
    Interpreters.oneFrame[IO](TestUtils.client(response, expectedRequest), config)

  val storage: InMemoryStorage[IO] = StorageAlgebra.inMemoryStorage

  def target(expectedRequest: Option[Request[IO]]) =
    ServicesGateway[IO](List(oneFrameService(response, expectedRequest)), storage, ttl)

  test("Gateway should use values from storage if ttl hasn't passed yet") {

    Given("test data in storage and expected request in service")
    storage.put(pair, Rate(pair, priceFromStorage, now)).unsafeRunSync()

    When("get result from Gateway")
    val result = target(None).get(pair).unsafeRunSync()

    Then("result should be from the storage")

    result.right shouldBe storage.get(pair).unsafeRunSync().right
  }

  test("Gateway should use values from service and update storage if ttl has passed") {

    Given("test data in storage and expected request in service")
    storage.put(pair, Rate(pair, priceFromStorage, now.minusMinutes(6))).unsafeRunSync()
    val expectedRequest: Request[IO] = Request(
      uri = uri"http://testhost:1111/rates?pair=USDEUR",
      headers = Headers.of(Header.apply("Token", config.tokens.head), Accept(MediaType.application.json))
    )
    When("get result from Gateway")
    val result = target(Some(expectedRequest)).get(pair).unsafeRunSync()

    Then("result should be from the service")
    result.right.get shouldBe Rate(pair, priceFromService, now)

    And("Storage should contain updated rate")
    storage.get(pair).unsafeRunSync().right.get shouldBe Rate(pair, priceFromService, now)
  }
}
