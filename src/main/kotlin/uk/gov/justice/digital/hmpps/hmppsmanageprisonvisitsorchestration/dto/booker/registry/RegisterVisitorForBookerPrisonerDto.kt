package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Details to register a visitor to a booker's prisoner.")
data class RegisterVisitorForBookerPrisonerDto(
  @JsonProperty("visitorId")
  @Schema(description = "Visitor Id", example = "12345", required = true)
  @field:NotBlank
  val visitorId: Long,

  @JsonProperty("active")
  @Schema(description = "Active / Inactive permitted visitor", example = "true", required = true)
  @NotNull
  val active: Boolean,

  @JsonProperty("notifyBookerFlag")
  @Schema(description = "Flag to determine if the booker should be notified of the registration", example = "true", required = false)
  @NotNull
  val notifyBookerFlag: Boolean? = false,
)
