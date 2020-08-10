package forex.validation.rates

import forex.domain.Currency
import forex.programs.rates.Protocol
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error.ValidationError
import forex.validation.simpleValidation
import org.scalatest.GivenWhenThen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SimpleValidationTest extends AnyFunSuite with Matchers with GivenWhenThen {

  val target = simpleValidation(List(Currency("USD"), Currency("EUR")))

  test("validation should passed when currency has correct length") {
    Given("request with incorrect currency length = 3")
    val request = Protocol.GetRatesRequest(Currency("USD"), Currency("EUR"))

    When("validate currency")
    val result = target.validate(request)

    Then("result should be Left")
    result shouldBe Right(GetRatesRequest(Currency("USD"), Currency("EUR")))
  }

  test("validation should fail when currency has incorrect length") {
    Given("request with incorrect currency length = 4")
    val request = Protocol.GetRatesRequest(Currency("USDD"), Currency("EUR"))

    When("validate currency")
    val result = target.validate(request)

    Then("result should be Left")
    result shouldBe Left(ValidationError("from", "Incorrect currency : USDD"))
  }

  test("validation should fail when currencies are the same") {
    Given("request with the same currency ")
    val request = Protocol.GetRatesRequest(Currency("EUR"), Currency("EUR"))

    When("validate currency")
    val result = target.validate(request)

    Then("result should be Left")
    result shouldBe Left(ValidationError("from and to", "Currencies are equal"))
  }

  test("validation should fail when currency not allowed") {
    Given("request with not allowed currency UAH")
    val request = Protocol.GetRatesRequest(Currency("UAH"), Currency("EUR"))

    When("validate currency")
    val result = target.validate(request)

    Then("result should be Left")
    result shouldBe Left(ValidationError("from", "Currency: UAH not allowed"))
  }
}
