package forex.validation.rates

import forex.domain.Currency
import forex.programs.rates.errors.Error.ValidationError
import forex.programs.rates.{Protocol, errors}
import forex.validation.rates.validators.{validateStringLength, validateStringsNonEqual}

class SimpleValidation(whitelist: List[Currency]) extends ValidationAlgebra {

  override def validate(request: Protocol.GetRatesRequest): Either[errors.Error, Protocol.GetRatesRequest] =
    (for {
      fromSized <- validateCurrencySize(request.from, "from")
      toSized <- validateCurrencySize(request.to, "to")
      from <- checkCurrencyNameInWhiteList(fromSized, "from", whitelist)
      to <- checkCurrencyNameInWhiteList(toSized, "to", whitelist)
      _ <- validateCurrencyNonEqual(from, to, "from" -> "to")
    } yield Protocol.GetRatesRequest(from, to))

  private def validateCurrencySize(curr: Currency, name: String): Either[errors.Error, Currency] =
    validateStringLength(curr.value, name, _ == 3, s"Incorrect currency : ${curr.value}").map(Currency(_))

  private def validateCurrencyNonEqual(curr1: Currency,
                                       curr2: Currency,
                                       names: (String, String)): Either[errors.Error, (Currency, Currency)] =
    validateStringsNonEqual(curr1.value, curr2.value, names, "Currencies are equal")
      .map(tuple => Currency(tuple._1) -> Currency(tuple._2))

  private def checkCurrencyNameInWhiteList(currency: Currency, field: String, wl: List[Currency]) =
    Either.cond(
      wl.contains(currency),
      currency,
      ValidationError(field, s"Currency: ${currency.value} not allowed")
    )
}

object SimpleValidation {
  def apply(whitelist: List[Currency]): SimpleValidation = new SimpleValidation(whitelist)

}
