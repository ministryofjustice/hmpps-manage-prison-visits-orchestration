package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import java.time.LocalDateTime

data class ChangeVisitSlotRequestDto(
  @Schema(description = "Visit Restriction", example = "OPEN", required = false)
  val visitRestriction: VisitRestriction? = null,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = false)
  val startTimestamp: LocalDateTime? = null,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = false)
  val endTimestamp: LocalDateTime? = null,
  @Schema(description = "Contact associated with the visit", required = false)
  @field:Valid
  val visitContact: ContactDto? = null,
  @Schema(description = "List of visitors associated with the visit", required = false)
  val visitors: Set<@Valid VisitorDto>? = null,
  @Schema(description = "List of additional support associated with the visit", required = false)
  val visitorSupport: Set<@Valid VisitorSupportDto>? = null,
)
