package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty

data class PrisonerReleasedInfo(
  @JsonProperty("nomsNumber")
  val prisonerNumber: String,
)
