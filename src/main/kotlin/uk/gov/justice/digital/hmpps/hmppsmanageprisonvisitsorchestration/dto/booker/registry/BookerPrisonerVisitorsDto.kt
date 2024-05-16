package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Visitor associated with the prisoner.")
data class BookerPrisonerVisitorsDto(
  @JsonProperty("visitorId")
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  @NotNull
  val visitorId: Long,

  @JsonProperty("active")
  @Schema(description = "Active / Inactive prisoner", example = "true", required = true)
  @NotNull
  val active: Boolean,
)
