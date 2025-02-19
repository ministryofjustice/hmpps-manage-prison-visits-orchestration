package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.AddressDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.RestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.EventAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import java.time.LocalDate
import java.time.LocalDateTime

data class VisitBookingDetailsDto internal constructor(
  @Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @Schema(description = "Visit Room", example = "Visits Main Hall", required = true)
  @field:NotBlank
  val visitRoom: String,
  @Schema(description = "Visit Type", example = "SOCIAL", required = true)
  val visitType: VisitType,
  @Schema(description = "Visit Status", example = "RESERVED", required = true)
  val visitStatus: VisitStatus,
  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val startTimestamp: LocalDateTime,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val endTimestamp: LocalDateTime,
  @Schema(description = "Contact associated with the visit", required = false)
  val visitContact: ContactDto? = null,
  @Schema(description = "Additional support associated with the visit", required = false)
  val visitorSupport: VisitorSupportDto? = null,
  @Schema(description = "Prison code and name", required = true)
  val prison: PrisonRegisterPrisonDto,
  @Schema(description = "Prisoner details", required = true)
  val prisoner: PrisonerDetailsDto,
  @Schema(description = "Prisoner details", required = true)
  val visitors: List<VisitorDetailsDto>,
  val events: List<EventAuditDto>,
  val notifications: List<VisitNotificationDto>,
) {
  constructor(
    visit: VisitDto,
    prison: PrisonRegisterPrisonDto,
    prisonerDto: PrisonerDto,
    prisonerAlerts: List<AlertDto>?,
    prisonerRestrictions: List<OffenderRestrictionDto>?,
    visitVisitors: List<PrisonerContactDto>,
    events: List<EventAuditDto>,
    // notifications: List<NotificationEventType>,
  ) : this(
    reference = visit.reference,
    visitRoom = visit.visitRoom,
    visitType = visit.visitType,
    visitStatus = visit.visitStatus,
    visitRestriction = visit.visitRestriction,
    startTimestamp = visit.startTimestamp,
    endTimestamp = visit.endTimestamp,
    visitContact = visit.visitContact,
    visitorSupport = visit.visitorSupport,
    prison = prison,
    prisoner = PrisonerDetailsDto(visit.prisonerId, prisonerDto, prisonerAlerts, prisonerRestrictions),
    visitors = visitVisitors.map { VisitorDetailsDto(it) }.toList(),
    events = events,
    notifications = emptyList(),
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

  @Schema(description = "In prison cell location", example = "A-1-002")
  val cellLocation: String? = null,

  @Schema(description = "Incentive level")
  val currentIncentive: CurrentIncentive? = null,

  @Schema(description = "Prisoner alerts")
  val prisonerAlerts: List<AlertDto>? = null,

  @Schema(description = "Prisoner restrictions")
  val prisonerRestrictions: List<OffenderRestrictionDto>? = null,
) {
  constructor(prisonerNumber: String, prisonerDto: PrisonerDto, prisonerAlerts: List<AlertDto>?, prisonerRestrictions: List<OffenderRestrictionDto>?) : this(
    prisonerNumber = prisonerNumber,
    firstName = prisonerDto.firstName,
    lastName = prisonerDto.lastName,
    dateOfBirth = prisonerDto.dateOfBirth,
    prisonId = prisonerDto.prisonId,
    cellLocation = prisonerDto.cellLocation,
    currentIncentive = prisonerDto.currentIncentive,
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
data class VisitNotificationDto(
  @Schema(description = "notification event type")
  val type: NotificationEventType,

  @Schema(description = "notification created at", example = "2018-12-01T13:45:00")
  val createTimestamp: LocalDateTime,

  @Schema(description = "notification additional data")
  val additionalData: Map<String, String>,
)
