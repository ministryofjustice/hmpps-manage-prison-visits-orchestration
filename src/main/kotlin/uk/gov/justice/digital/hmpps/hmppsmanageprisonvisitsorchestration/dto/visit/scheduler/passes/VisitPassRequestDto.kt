package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class VisitPassRequestDto(
  @param:Schema(description = "Date for which visit passes are being sought.", example = "2025-01-01", required = true)
  @field:NotNull
  val visitDate: LocalDate,

  @param:Schema(description = "STAFF username who triggered the visit passes endpoint.", example = "ABC123D", required = true)
  @field:NotBlank
  val actionedBy: String,
)
