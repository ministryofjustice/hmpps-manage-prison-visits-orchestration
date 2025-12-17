package uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.EventAuditDto
import java.time.LocalDateTime

@Schema(description = "Combined visits and booker registry audit entries for a public booker.")
data class BookerHistoryAuditDto(
  @param:Schema(name = "auditType", description = "Audit Type", required = true, example = "PRISONER_REGISTERED")
  val auditType: String,

  @param:Schema(name = "text", description = "Audit summary", required = false)
  val text: String? = null,

  @param:Schema(name = "createdTimestamp", description = "Timestamp of booker audit entry", required = true)
  val createdTimestamp: LocalDateTime,
) {
  constructor(eventAuditDto: EventAuditDto) : this(
    auditType = eventAuditDto.type.name,
    text = eventAuditDto.text,
    createdTimestamp = eventAuditDto.createTimestamp,
  )

  constructor(bookerAuditDto: BookerAuditDto) : this(
    auditType = bookerAuditDto.auditType,
    text = bookerAuditDto.text,
    createdTimestamp = bookerAuditDto.createdTimestamp,
  )
}
