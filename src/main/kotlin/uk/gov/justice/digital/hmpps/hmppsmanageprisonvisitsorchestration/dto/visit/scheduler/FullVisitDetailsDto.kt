package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import java.time.LocalDateTime

@Schema(description = "Full Visit Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
class FullVisitDetailsDto(
  applicationReference: String,
  reference: String,
  prisonerId: String,
  prisonCode: String,
  sessionTemplateReference: String? = null,
  visitRoom: String,
  visitType: VisitType,
  visitStatus: VisitStatus,
  outcomeStatus: OutcomeStatus?,
  visitRestriction: VisitRestriction,
  startTimestamp: LocalDateTime,
  endTimestamp: LocalDateTime,
  visitNotes: List<VisitNoteDto>? = listOf(),
  visitContact: ContactDto?,
  override val visitors: List<FullVisitorDetailsDto>? = listOf(),
  visitorSupport: List<VisitorSupportDto>? = listOf(),
  createdBy: String,
  updatedBy: String?,
  cancelledBy: String?,
  createdTimestamp: LocalDateTime,
  modifiedTimestamp: LocalDateTime,
  @Schema(description = "Prison name", example = "MDI Prison", required = false)
  var prisonName: String? = null,
) : VisitDto(
  applicationReference = applicationReference,
  reference = reference,
  prisonerId = prisonerId,
  prisonCode = prisonCode,
  sessionTemplateReference = sessionTemplateReference,
  visitRoom = visitRoom,
  visitType = visitType,
  visitStatus = visitStatus,
  outcomeStatus = outcomeStatus,
  visitRestriction = visitRestriction,
  startTimestamp = startTimestamp,
  endTimestamp = endTimestamp,
  visitNotes = visitNotes,
  visitContact = visitContact,
  visitors = visitors,
  visitorSupport = visitorSupport,
  createdBy = createdBy,
  updatedBy = updatedBy,
  cancelledBy = cancelledBy,
  createdTimestamp = createdTimestamp,
  modifiedTimestamp = modifiedTimestamp,
) {
  constructor(
    visitDto: VisitDto,
  ) :
    this(
      applicationReference = visitDto.applicationReference,
      reference = visitDto.reference,
      prisonerId = visitDto.prisonerId,
      prisonCode = visitDto.prisonCode,
      sessionTemplateReference = visitDto.sessionTemplateReference,
      visitRoom = visitDto.visitRoom,
      visitType = visitDto.visitType,
      visitStatus = visitDto.visitStatus,
      outcomeStatus = visitDto.outcomeStatus,
      visitRestriction = visitDto.visitRestriction,
      startTimestamp = visitDto.startTimestamp,
      endTimestamp = visitDto.endTimestamp,
      visitNotes = visitDto.visitNotes,
      visitContact = visitDto.visitContact,
      visitors = visitDto.visitors?.map { FullVisitorDetailsDto(it) },
      visitorSupport = visitDto.visitorSupport,
      createdBy = visitDto.createdBy,
      updatedBy = visitDto.updatedBy,
      cancelledBy = visitDto.cancelledBy,
      createdTimestamp = visitDto.createdTimestamp,
      modifiedTimestamp = visitDto.modifiedTimestamp,
    )
}
