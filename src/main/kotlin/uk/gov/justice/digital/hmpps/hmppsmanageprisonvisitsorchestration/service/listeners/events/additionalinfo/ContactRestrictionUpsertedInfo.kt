package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

data class ContactRestrictionUpsertedInfo(
  var contactId: String?,

  val contactRestrictionId: String,
) : EventInfo
