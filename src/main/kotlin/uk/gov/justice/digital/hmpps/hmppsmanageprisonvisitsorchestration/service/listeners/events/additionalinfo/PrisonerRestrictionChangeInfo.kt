package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty

data class PrisonerRestrictionChangeInfo(
  @JsonProperty("nomsNumber")
  val prisonerNumber: String,
)
