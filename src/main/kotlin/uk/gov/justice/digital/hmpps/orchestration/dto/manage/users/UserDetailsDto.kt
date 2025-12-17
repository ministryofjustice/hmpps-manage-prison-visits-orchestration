package uk.gov.justice.digital.hmpps.orchestration.dto.manage.users

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class UserDetailsDto(
  @param:Schema(description = "username", example = "DEMO_USER1", required = true)
  @field:NotNull
  val username: String,

  @param:Schema(description = "Full name", example = "John Smith", required = false)
  @param:JsonProperty("name")
  val fullName: String? = null,
)
