package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.UserType

@Schema(description = "Actioned By")
data class ActionedByDto(

  @param:Schema(description = "booker reference", example = "asd-aed-vhj", required = false)
  val bookerReference: String?,

  @param:Schema(description = "User Name", example = "AS/ALED", required = false)
  val userName: String?,

  @param:Schema(description = "User type", example = "STAFF", required = false)
  @field:NotNull
  val userType: UserType,
)
