package forex

import forex.validation.rates.{ SimpleValidation, ValidationAlgebra }

package object validation {
  type ValidationService = ValidationAlgebra
  final val simpleValidation = SimpleValidation
}
