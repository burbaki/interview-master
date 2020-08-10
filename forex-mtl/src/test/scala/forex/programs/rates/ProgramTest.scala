package forex.programs.rates

import java.time.OffsetDateTime

import cats.Applicative
import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{Currency, Price, Rate}
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.ValidationError
import forex.services.rates.ServiceAlgebra
import forex.services.rates.errors.ServiceError
import forex.validation.{ValidationService, simpleValidation}
import org.scalatest.GivenWhenThen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
class ProgramTest extends AnyFunSuite with Matchers with GivenWhenThen {

  val time: OffsetDateTime = OffsetDateTime.now
  val defaultPrice: Price  = Price(BigDecimal(100))

  val validation: ValidationService = simpleValidation(List(Currency("EUR"), Currency("USD")))

  def serviceStub[F[_]: Applicative] = new ServiceAlgebra[F] {
    override def get(pair: Rate.Pair): F[Either[ServiceError, Rate]] =
      Rate(pair, defaultPrice, time).asRight[ServiceError].pure[F]
  }

  val targetProgram: RatesProgram[IO] = RatesProgram[IO](serviceStub, validation)

  test("correct input should return correct result") {
    Given("have correct request")
    val request = Protocol.GetRatesRequest(Currency("USD"), Currency("EUR"))

    When("pass request to the program and complete")
    val result = targetProgram.get(request).unsafeRunSync()

    Then("result is Right ")
    result shouldBe Right(Rate(Rate.Pair(Currency("USD"), Currency("EUR")), defaultPrice, time))
  }

  test("incorrect input should return validation error") {
    Given("have correct request")
    val request = Protocol.GetRatesRequest(Currency("USDD"), Currency("EUR"))

    When("pass request to the program and complete")
    val result = targetProgram.get(request).unsafeRunSync()

    Then("result is Left with error ")
    result shouldBe Left(ValidationError("from", "Incorrect currency : USDD"))
  }
}
