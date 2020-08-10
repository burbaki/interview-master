package forex.cache.rates

object errors {

  sealed trait StorageError {
    val msg: String
  }

  final case class NotFoundPair(msg: String) extends StorageError

}
