package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty

data class PrisonerAlertAddedInfo(
  var prisonerNumber: String?,

  val alertCode: String,

  @param:JsonProperty("alertUuid")
  val alertUUID: String,
) : EventInfo
