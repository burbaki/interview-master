package forex.validation.rates

import forex.programs.rates.errors
import forex.programs.rates.errors.Error.ValidationError

// This should be in commons repository, etc
object validators {

  def validateStringLength(str: String,
                           name: String,
                           sizePredicate: Int => Boolean,
                           msg: String = "incorrectSize"): Either[errors.Error, String] =
    Either.cond(sizePredicate(str.length), str, ValidationError(name, msg))

  def validateStringsNonEqual(str1: String,
                              str2: String,
                              names: (String, String),
                              msg: String = "values should not be the same"): Either[errors.Error, (String, String)] =
    Either.cond(str1 != str2, str1 -> str2, ValidationError(s"${names._1} and ${names._2}", msg))
}
