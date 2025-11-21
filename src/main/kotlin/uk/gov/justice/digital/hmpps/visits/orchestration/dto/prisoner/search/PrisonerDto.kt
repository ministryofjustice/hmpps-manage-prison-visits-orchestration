package uk.gov.justice.digital.hmpps.visits.orchestration.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonerDto(
  @param:Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String,

  @param:Schema(required = true, description = "First Name", example = "Robert")
  val firstName: String,

  @param:Schema(required = true, description = "Last name", example = "Larsen")
  val lastName: String,

  @param:Schema(required = true, description = "Date of Birth", example = "1975-04-02")
  val dateOfBirth: LocalDate,

  @param:Schema(description = "Prison ID", example = "MDI")
  val prisonId: String?,

  @param:Schema(description = "Prison Name", example = "HMP Leeds")
  val prisonName: String?,

  @param:Schema(description = "In prison cell location", example = "A-1-002")
  val cellLocation: String? = null,

  @param:Schema(description = "Incentive level")
  val currentIncentive: CurrentIncentive? = null,

  @param:Schema(description = "current prison or outside with last movement information.", example = "Outside - released from Leeds")
  val locationDescription: String? = null,

  @param:Schema(description = "Convicted Status", example = "Convicted", allowableValues = ["Convicted", "Remand"])
  val convictedStatus: String? = null,
)
