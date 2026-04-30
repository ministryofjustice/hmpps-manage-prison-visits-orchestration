package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "STAFF user details")
data class StaffUsernameDto(
  @field:NotBlank
  @param:Schema(description = "User Name for STAFF", example = "ALED", required = true)
  val username: String,
)
