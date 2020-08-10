package forex.programs.rates

import forex.domain.Rate
import errors._

trait ProgramAlgebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): F[Error Either Rate]
}
