package forex.validation.rates

import forex.programs.rates.Protocol
import forex.programs.rates.errors.Error

trait ValidationAlgebra {
  def validate(request: Protocol.GetRatesRequest): Either[Error, Protocol.GetRatesRequest]
}
