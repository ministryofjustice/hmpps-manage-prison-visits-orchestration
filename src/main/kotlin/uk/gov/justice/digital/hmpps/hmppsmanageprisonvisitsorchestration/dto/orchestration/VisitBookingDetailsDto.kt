package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.AddressDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.RestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitNoteDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventAttributeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.VisitNotificationEventDto
import java.time.LocalDate
import java.time.LocalDateTime

data class VisitBookingDetailsDto internal constructor(
  @Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @Schema(description = "Visit Room", example = "Visits Main Hall", required = true)
  @field:NotBlank
  val visitRoom: String,
  @Schema(description = "Visit Status", example = "RESERVED", required = true)
  val visitStatus: VisitStatus,
  @Schema(description = "Outcome Status", example = "VISITOR_CANCELLED", required = false)
  val outcomeStatus: OutcomeStatus?,
  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val startTimestamp: LocalDateTime,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val endTimestamp: LocalDateTime,
  @Schema(description = "Visit Notes", required = false)
  val visitNotes: List<VisitNoteDto>? = listOf(),
  @Schema(description = "Contact associated with the visit", required = false)
  val visitContact: VisitContactDto? = null,
  @Schema(description = "Additional support associated with the visit", required = false)
  val visitorSupport: VisitorSupportDto? = null,
  @Schema(description = "Prison code and name", required = true)
  val prison: PrisonRegisterPrisonDto,
  @Schema(description = "Prisoner details", required = true)
  val prisoner: PrisonerDetailsDto,
  @Schema(description = "Prisoner details", required = true)
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
    outcomeStatus = visit.outcomeStatus,
    visitRestriction = visit.visitRestriction,
    startTimestamp = visit.startTimestamp,
    endTimestamp = visit.endTimestamp,
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

data class PrisonerDetailsDto internal constructor(
  @Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(required = true, description = "First Name", example = "Robert")
  val firstName: String,

  @Schema(required = true, description = "Last name", example = "Larsen")
  val lastName: String,

  @Schema(required = true, description = "Date of Birth", example = "1975-04-02")
  val dateOfBirth: LocalDate,

  @Schema(description = "Prison ID", example = "MDI")
  val prisonId: String?,

  @Schema(description = "Prison Name", example = "HMP Leeds")
  val prisonName: String?,

  @Schema(description = "In prison cell location", example = "A-1-002")
  val cellLocation: String? = null,

  @Schema(description = "current prison or outside with last movement information.", example = "Outside - released from Leeds")
  val locationDescription: String? = null,

  @Schema(description = "Prisoner alerts")
  val prisonerAlerts: List<AlertDto> = emptyList(),

  @Schema(description = "Prisoner restrictions")
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
data class VisitorDetailsDto internal constructor(
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791")
  val personId: Long? = null,
  @Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
  @Schema(description = "Date of birth", example = "1980-01-28", required = false)
  val dateOfBirth: LocalDate? = null,
  @Schema(description = "Description of relationship to Prisoner", example = "Responsible Officer", required = false)
  val relationshipDescription: String? = null,
  @Schema(description = "List of restrictions associated with the contact", required = false)
  val restrictions: List<RestrictionDto> = listOf(),
  @Schema(description = "List of addresses associated with the contact", required = false)
  val primaryAddress: AddressDto?,
) {
  constructor(prisonerContactDto: PrisonerContactDto) : this(
    personId = prisonerContactDto.personId,
    firstName = prisonerContactDto.firstName,
    lastName = prisonerContactDto.lastName,
    dateOfBirth = prisonerContactDto.dateOfBirth,
    relationshipDescription = prisonerContactDto.relationshipDescription,
    restrictions = prisonerContactDto.restrictions,
    primaryAddress = prisonerContactDto.addresses.firstOrNull { it.primary },
  )
}

@Schema(description = "Visit notification details")
data class VisitNotificationDto internal constructor(
  @Schema(description = "notification event type")
  val type: NotificationEventType,

  @Schema(description = "notification created at", example = "2018-12-01T13:45:00")
  val createdDateTime: LocalDateTime,

  @Schema(description = "notification additional data")
  val additionalData: List<VisitNotificationEventAttributeDto>,
) {
  constructor(visitNotificationEventDto: VisitNotificationEventDto) : this(
    type = visitNotificationEventDto.type,
    createdDateTime = visitNotificationEventDto.createdDateTime,
    additionalData = visitNotificationEventDto.additionalData,
  )
}

@Schema(description = "Visit notification details")
data class VisitContactDto internal constructor(
  @Schema(description = "Main contact ID associated with the visit", example = "1234", required = false)
  val visitContactId: Long?,
  @Schema(description = "Contact Name", example = "John Smith", required = true)
  override val name: String,
  @Schema(description = "Contact Phone Number", example = "01234 567890", required = false)
  override val telephone: String? = null,
  @Schema(description = "Contact Email Address", example = "email@example.com", required = false)
  override val email: String? = null,
) : ContactDto(name = name, telephone = telephone, email = email) {
  constructor(contactDto: ContactDto, visitContactId: Long?) : this(
    visitContactId = visitContactId,
    name = contactDto.name,
    telephone = contactDto.telephone,
    email = contactDto.email,
  )
}
