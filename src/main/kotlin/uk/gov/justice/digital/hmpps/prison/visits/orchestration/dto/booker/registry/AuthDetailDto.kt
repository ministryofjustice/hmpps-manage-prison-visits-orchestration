package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Auth detail Dto")
data class AuthDetailDto(

  @param:Schema(name = "oneLoginSub", description = "auth reference/sub", required = true)
  @field:NotBlank
  val oneLoginSub: String,

  @param:Schema(name = "email", description = "auth email", required = true)
  @field:NotBlank
  val email: String,

  @param:Schema(name = "phoneNumber", description = "auth phone number", required = false)
  val phoneNumber: String? = null,

)
