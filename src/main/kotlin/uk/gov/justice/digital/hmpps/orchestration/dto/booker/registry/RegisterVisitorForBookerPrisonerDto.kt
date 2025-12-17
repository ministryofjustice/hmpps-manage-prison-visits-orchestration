package uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Details to register a visitor to a booker's prisoner.")
data class RegisterVisitorForBookerPrisonerDto(
  @param:Schema(description = "Visitor Id", example = "12345", required = true)
  @field:NotBlank
  val visitorId: Long,

  @param:Schema(description = "Flag to determine if the booker should be notified of the registration", example = "true", required = false)
  @field:NotNull
  val sendNotificationFlag: Boolean? = false,
)
