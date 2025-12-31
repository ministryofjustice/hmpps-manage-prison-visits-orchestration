package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType

@Schema(description = "Prison user client dto")
data class PrisonUserClientDto(

  @param:Schema(description = "User type", example = "STAFF", required = true)
  @field:NotNull
  val userType: UserType,

  @param:Schema(description = "minimum number of days notice from the current date to booked a visit", example = "2", required = true)
  val policyNoticeDaysMin: Int,

  @param:Schema(description = "maximum number of days notice from the current date to booked a visit", example = "28", required = true)
  val policyNoticeDaysMax: Int,

  @param:Schema(description = "is prison user client active", example = "true", required = true)
  @field:NotNull
  var active: Boolean,
)
