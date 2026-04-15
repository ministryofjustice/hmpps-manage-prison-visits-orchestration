package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.ContactRestrictionUpsertedInfo

data class ContactRestrictionUpsertedNotificationDto(
  val contactId: Long,

  val restrictionId: Long,
) {
  constructor(info: ContactRestrictionUpsertedInfo) : this(
    contactId = requireNotNull(info.contactId) { "Contact ID is required" }.toLong(),
    restrictionId = info.contactRestrictionId.toLong(),
  )
}
