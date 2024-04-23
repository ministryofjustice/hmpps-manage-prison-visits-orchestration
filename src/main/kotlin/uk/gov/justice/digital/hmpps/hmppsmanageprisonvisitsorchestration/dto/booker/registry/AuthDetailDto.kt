package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Auth detail Dto")
data class AuthDetailDto(

  @Schema(name = "oneLoginSub", description = "auth reference/sub", required = true)
  @field:NotBlank
  val oneLoginSub: String,

  @Schema(name = "email", description = "auth email", required = true)
  @field:NotBlank
  val email: String,

  @Schema(name = "phoneNumber", description = "auth phone number", required = false)
  val phoneNumber: String? = null,

)
