package forex.programs.rates

import forex.services.rates.errors._

object errors {

  sealed trait Error extends Throwable

  object Error {

    final case class RateLookupFailed(errorType: String, msg: String) extends Error

    final case class ValidationError(field: String, msg: String) extends Error
  }

  def toProgramError(error: ServiceError): Error = error match {
    case err: ServiceError => Error.RateLookupFailed(err.getClass.getName, err.msg)
  }

}
