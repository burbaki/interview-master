package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import cats.syntax.applicative._
import forex.domain._
import forex.programs.rates.errors.{toProgramError, _}
import forex.services.RatesService
import forex.validation.ValidationService

class Program[F[_]: Monad](
                            ratesService: RatesService[F],
                            validation: ValidationService,
) extends ProgramAlgebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] =
    (for {
      validated <- EitherT(validation.validate(request).pure[F])
      serviceResult <- EitherT(ratesService.get(Rate.Pair(validated.from, validated.to)))
                        .leftMap(toProgramError)
    } yield serviceResult).value
}

object Program {

  def apply[F[_]: Monad](
                          ratesService: RatesService[F],
                          validation: ValidationService,
  ): ProgramAlgebra[F] = new Program[F](ratesService, validation)
}
