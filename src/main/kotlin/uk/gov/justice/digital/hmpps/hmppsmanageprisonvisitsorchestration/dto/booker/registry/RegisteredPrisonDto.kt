package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto

data class RegisteredPrisonDto(
  @Schema(description = "prison code", example = "MDI", required = true)
  val prisonCode: String,

  @Schema(description = "prison name", example = "MDI", required = true)
  val prisonName: String,
) {
  constructor(prisonDto: PrisonRegisterPrisonDto) : this(
    prisonCode = prisonDto.prisonId,
    prisonName = prisonDto.prisonName,
  )
}
