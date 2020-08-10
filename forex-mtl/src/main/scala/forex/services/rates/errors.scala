package forex.services.rates

import forex.cache.rates.errors.StorageError

object errors {

  sealed trait ServiceError {
    val msg: String
  }
  object Error {

    final case class ClientResponseParseError(msg: String) extends ServiceError

    final case class LookupFailed(msg: String) extends ServiceError

    final case class NotFoundCurrency(msg: String) extends ServiceError

    final case class WrongUri(msg: String) extends ServiceError

    final case class StorageErrorWrapped(msg: String) extends ServiceError

    def toServiceError(error: StorageError): StorageErrorWrapped = error match {
      case err => Error.StorageErrorWrapped(err.msg)
    }
  }

}
