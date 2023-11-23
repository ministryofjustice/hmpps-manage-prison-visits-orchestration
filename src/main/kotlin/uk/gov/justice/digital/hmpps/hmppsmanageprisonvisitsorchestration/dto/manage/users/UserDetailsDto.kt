package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.manage.users

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class UserDetailsDto(
  @Schema(description = "username", example = "DEMO_USER1", required = true)
  @field:NotNull
  val username: String,

  @Schema(description = "Full name", example = "John Smith", required = false)
  @JsonProperty("name")
  val fullName: String? = null,
)
