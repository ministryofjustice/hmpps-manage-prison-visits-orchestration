package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

class ReserveVisitSlotDto(
  @Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  @field:NotBlank
  val prisonerId: String,
  @JsonProperty("prisonId")
  @Schema(description = "Prison Id", example = "MDI", required = true)
  @field:NotBlank
  val prisonCode: String,
  @Schema(description = "Visit Room", example = "A1", required = true)
  @field:NotBlank
  val visitRoom: String,
  @Schema(description = "Visit Type", example = "SOCIAL", required = true)
  @field:NotNull
  val visitType: String,
  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  @field:NotNull
  val visitRestriction: String,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val startTimestamp: LocalDateTime,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val endTimestamp: LocalDateTime,
  @Schema(description = "Contact associated with the visit", required = false)
  @field:Valid
  val visitContact: ContactDto?,
  @Schema(description = "List of visitors associated with the visit", required = true)
  @field:NotEmpty
  val visitors: Set<@Valid VisitorDto>,
  @Schema(description = "List of additional support associated with the visit", required = false)
  val visitorSupport: Set<@Valid VisitorSupportDto>? = setOf(),
)
