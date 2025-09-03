package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Audit entry for booker.")
data class BookerAuditDto(
  @Schema(name = "reference", description = "Booker reference", required = true)
  val bookerReference: String,

  @Schema(name = "auditType", description = "Audit Type", required = true, example = "PRISONER_REGISTERED")
  val auditType: String,

  @Schema(name = "text", description = "Audit summary", required = true)
  val text: String,

  @Schema(name = "createdTimestamp", description = "Timestamp of booker audit entry", required = true)
  val createdTimestamp: LocalDateTime,
)
