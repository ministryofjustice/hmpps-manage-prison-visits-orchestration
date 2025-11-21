package uk.gov.justice.digital.hmpps.visits.orchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.contact.registry.AddressDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.contact.registry.RestrictionDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.VisitNoteDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.VisitSubStatus
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.VisitorSupportDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventAttributeDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventDto
import java.time.LocalDate
import java.time.LocalDateTime

data class VisitBookingDetailsDto(
  @param:Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @param:Schema(description = "Visit Room", example = "Visits Main Hall", required = true)
  @field:NotBlank
  val visitRoom: String,
  @param:Schema(description = "Visit Status", example = "RESERVED", required = true)
  val visitStatus: VisitStatus,
  @param:Schema(description = "Visit Sub Status", example = "AUTO_APPROVED", required = true)
  val visitSubStatus: VisitSubStatus,
  @param:Schema(description = "Outcome Status", example = "VISITOR_CANCELLED", required = false)
  val outcomeStatus: OutcomeStatus?,
  @param:Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @param:Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val startTimestamp: LocalDateTime,
  @param:Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val endTimestamp: LocalDateTime,
  @param:Schema(description = "Session Template Reference", example = "v9d.7ed.7u", required = false)
  val sessionTemplateReference: String? = null,
  @param:Schema(description = "Visit Notes", required = false)
  val visitNotes: List<VisitNoteDto>? = listOf(),
  @param:Schema(description = "Contact associated with the visit", required = false)
  val visitContact: VisitContactDto? = null,
  @param:Schema(description = "Additional support associated with the visit", required = false)
  val visitorSupport: VisitorSupportDto? = null,
  @param:Schema(description = "Prison code and name", required = true)
  val prison: PrisonRegisterPrisonDto,
  @param:Schema(description = "Prisoner details", required = true)
  val prisoner: PrisonerDetailsDto,
  @param:Schema(description = "Prisoner details", required = true)
  val visitors: List<VisitorDetailsDto>,
  val events: List<EventAuditOrchestrationDto>,
  val notifications: List<VisitNotificationDto>,
) {
  constructor(
    visit: VisitDto,
    prison: PrisonRegisterPrisonDto,
    prisonerDto: PrisonerDto,
    prisonerAlerts: List<AlertDto>,
    prisonerRestrictions: List<OffenderRestrictionDto>,
    visitVisitors: List<PrisonerContactDto>,
    visitContact: VisitContactDto?,
    events: List<EventAuditOrchestrationDto>,
    notifications: List<VisitNotificationEventDto>,
  ) : this(
    reference = visit.reference,
    visitRoom = visit.visitRoom,
    visitStatus = visit.visitStatus,
    visitSubStatus = visit.visitSubStatus,
    outcomeStatus = visit.outcomeStatus,
    visitRestriction = visit.visitRestriction,
    startTimestamp = visit.startTimestamp,
    endTimestamp = visit.endTimestamp,
    sessionTemplateReference = visit.sessionTemplateReference,
    visitNotes = visit.visitNotes,
    visitContact = visitContact,
    visitorSupport = visit.visitorSupport,
    prison = prison,
    prisoner = PrisonerDetailsDto(visit.prisonerId, prisonerDto, prisonerAlerts, prisonerRestrictions),
    visitors = visitVisitors.map { VisitorDetailsDto(it) }.toList(),
    events = events,
    notifications = notifications.map { VisitNotificationDto(it) },
  )
}

data class PrisonerDetailsDto(
  @param:Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String,

  @param:Schema(required = true, description = "First Name", example = "Robert")
  val firstName: String,

  @param:Schema(required = true, description = "Last name", example = "Larsen")
  val lastName: String,

  @param:Schema(required = true, description = "Date of Birth", example = "1975-04-02")
  val dateOfBirth: LocalDate,

  @param:Schema(description = "Prison ID", example = "MDI")
  val prisonId: String?,

  @param:Schema(description = "Prison Name", example = "HMP Leeds")
  val prisonName: String?,

  @param:Schema(description = "In prison cell location", example = "A-1-002")
  val cellLocation: String? = null,

  @param:Schema(description = "current prison or outside with last movement information.", example = "Outside - released from Leeds")
  val locationDescription: String? = null,

  @param:Schema(description = "Prisoner alerts")
  val prisonerAlerts: List<AlertDto> = emptyList(),

  @param:Schema(description = "Prisoner restrictions")
  val prisonerRestrictions: List<OffenderRestrictionDto> = emptyList(),
) {
  constructor(prisonerNumber: String, prisonerDto: PrisonerDto, prisonerAlerts: List<AlertDto>, prisonerRestrictions: List<OffenderRestrictionDto>) : this(
    prisonerNumber = prisonerNumber,
    firstName = prisonerDto.firstName,
    lastName = prisonerDto.lastName,
    dateOfBirth = prisonerDto.dateOfBirth,
    prisonId = prisonerDto.prisonId,
    prisonName = prisonerDto.prisonName,
    cellLocation = prisonerDto.cellLocation,
    locationDescription = prisonerDto.locationDescription,
    prisonerAlerts = prisonerAlerts,
    prisonerRestrictions = prisonerRestrictions,
  )
}

@Schema(description = "Visitor details")
data class VisitorDetailsDto(
  @param:Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791")
  val personId: Long? = null,
  @param:Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @param:Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
  @param:Schema(description = "Date of birth", example = "1980-01-28", required = false)
  val dateOfBirth: LocalDate? = null,
  @param:Schema(description = "Description of relationship to Prisoner", example = "Responsible Officer", required = false)
  val relationshipDescription: String? = null,
  @param:Schema(description = "List of restrictions associated with the contact", required = false)
  val restrictions: List<RestrictionDto> = listOf(),
  @param:Schema(description = "Primary address for the contact or the first address if no primary address available, null if address list is empty", required = false)
  val primaryAddress: AddressDto?,
) {
  constructor(prisonerContactDto: PrisonerContactDto) : this(
    personId = prisonerContactDto.personId,
    firstName = prisonerContactDto.firstName,
    lastName = prisonerContactDto.lastName,
    dateOfBirth = prisonerContactDto.dateOfBirth,
    relationshipDescription = prisonerContactDto.relationshipDescription,
    restrictions = prisonerContactDto.restrictions,
    // first primary address or first address in the list or null
    primaryAddress = prisonerContactDto.addresses.firstOrNull { it.primary } ?: prisonerContactDto.addresses.firstOrNull(),
  )
}

@Schema(description = "Visit notification details")
data class VisitNotificationDto(
  @param:Schema(description = "notification event type")
  val type: NotificationEventType,

  @param:Schema(description = "notification created at", example = "2018-12-01T13:45:00")
  val createdDateTime: LocalDateTime,

  @param:Schema(description = "notification additional data")
  val additionalData: List<VisitNotificationEventAttributeDto>,
) {
  constructor(visitNotificationEventDto: VisitNotificationEventDto) : this(
    type = visitNotificationEventDto.type,
    createdDateTime = visitNotificationEventDto.createdDateTime,
    additionalData = visitNotificationEventDto.additionalData,
  )
}

@Schema(description = "Visit notification details")
data class VisitContactDto(
  @param:Schema(description = "Main contact ID associated with the visit", example = "1234", required = false)
  val visitContactId: Long?,
  @param:Schema(description = "Contact Name", example = "John Smith", required = true)
  override val name: String,
  @param:Schema(description = "Contact Phone Number", example = "01234 567890", required = false)
  override val telephone: String? = null,
  @param:Schema(description = "Contact Email Address", example = "email@example.com", required = false)
  override val email: String? = null,
) : ContactDto(name = name, telephone = telephone, email = email) {
  constructor(contactDto: ContactDto, visitContactId: Long?) : this(
    visitContactId = visitContactId,
    name = contactDto.name,
    telephone = contactDto.telephone,
    email = contactDto.email,
  )
}
