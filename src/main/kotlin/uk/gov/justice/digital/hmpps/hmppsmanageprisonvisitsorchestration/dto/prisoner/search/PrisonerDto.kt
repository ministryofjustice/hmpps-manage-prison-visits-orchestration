package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

open class PrisonerDto(
  @Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  @JsonAlias("prisonerNumber")
  val prisonerId: String,

  @Schema(required = true, description = "First Name", example = "Robert")
  val firstName: String,

  @Schema(required = true, description = "Last name", example = "Larsen")
  val lastName: String,

  @Schema(required = true, description = "Date of Birth", example = "1975-04-02")
  val dateOfBirth: LocalDate,

  @Schema(description = "Prison ID", example = "MDI")
  val prisonId: String?,

  @Schema(description = "Prison Name", example = "HMP Leeds")
  val prisonName: String?,

  @Schema(description = "In prison cell location", example = "A-1-002")
  val cellLocation: String? = null,

  @Schema(description = "Incentive level")
  val currentIncentive: CurrentIncentive? = null,
)
