package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.hmpps.auth

import io.swagger.v3.oas.annotations.media.Schema

data class UserDetails(
  @Schema(description = "username", example = "DEMO_USER1", required = true)
  val username: String,

  @Schema(description = "Full name", example = "John Smith", required = true)
  val name: String,
)
