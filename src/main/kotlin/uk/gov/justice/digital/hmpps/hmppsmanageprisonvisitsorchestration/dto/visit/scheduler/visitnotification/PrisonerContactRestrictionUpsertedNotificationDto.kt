package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerContactRestrictionUpsertedInfo

data class PrisonerContactRestrictionUpsertedNotificationDto(
  @field:NotBlank
  val prisonerNumber: String,

  val contactId: Long,

  val prisonerContactId: Long,

  val restrictionId: Long,
) {
  constructor(info: PrisonerContactRestrictionUpsertedInfo) : this(
    prisonerNumber = requireNotNull(info.prisonerNumber) { "Prisoner number is required" },
    contactId = requireNotNull(info.contactId) { "Contact ID is required" }.toLong(),
    prisonerContactId = info.prisonerContactId.toLong(),
    restrictionId = info.prisonerContactRestrictionId.toLong(),
  )
}
