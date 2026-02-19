package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Staff user details returned from Manage Users API.")
data class UserExtendedDetailsDto(
  @param:Schema(description = "username", example = "ABCD123A", required = true)
  @field:NotNull
  val username: String,

  @param:Schema(description = "firstName", example = "John", required = true)
  @field:NotNull
  val firstName: String,

  @param:Schema(description = "firstName", example = "John", required = true)
  @field:NotNull
  val lastName: String,
)
