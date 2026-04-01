package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto

data class RegisteredPrisonDto(
  @param:Schema(description = "prison code", example = "MDI", required = true)
  val prisonCode: String,

  @param:Schema(description = "prison name", example = "MDI", required = true)
  val prisonName: String,

  @param:Schema(description = "Name of the prison in Welsh", example = "Carchar Brynbuga", required = false)
  val prisonNameInWelsh: String? = null,
) {
  constructor(prisonDto: PrisonRegisterPrisonDto) : this(
    prisonCode = prisonDto.prisonId,
    prisonName = prisonDto.prisonName,
    prisonNameInWelsh = prisonDto.prisonNameInWelsh,
  )
}
