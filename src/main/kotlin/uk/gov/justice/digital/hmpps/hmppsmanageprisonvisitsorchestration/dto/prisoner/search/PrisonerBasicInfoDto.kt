package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonerBasicInfoDto(
  @Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(required = true, description = "First Name", example = "Robert")
  val firstName: String,

  @Schema(required = true, description = "Last name", example = "Larsen")
  val lastName: String,

  @Schema(required = true, description = "Date of Birth", example = "1975-04-02")
  val dateOfBirth: LocalDate?,

  @Schema(description = "Prison ID", example = "MDI")
  val prisonId: String?,
)
