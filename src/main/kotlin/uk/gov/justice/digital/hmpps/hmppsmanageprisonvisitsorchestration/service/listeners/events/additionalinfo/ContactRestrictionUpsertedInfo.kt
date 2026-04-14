package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

data class ContactRestrictionUpsertedInfo(
  var prisonerNumber: String?,

  var contactId: String?,

  val prisonerContactId: String,

  val prisonerContactRestrictionId: String,
) : EventInfo
