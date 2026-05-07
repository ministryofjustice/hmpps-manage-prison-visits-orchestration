package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

data class PrisonerAlertAddedInfo(
  var prisonerNumber: String?,

  val alertCode: String,

  val alertUuid: String,
) : EventInfo
