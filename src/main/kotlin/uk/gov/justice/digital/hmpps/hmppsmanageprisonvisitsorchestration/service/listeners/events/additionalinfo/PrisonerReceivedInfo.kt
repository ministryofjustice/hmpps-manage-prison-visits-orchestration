package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PrisonerReceivedInfo(
  @NotBlank
  @JsonProperty("nomsNumber")
  val prisonerNumber: String,
  @NotBlank
  @JsonProperty("prisonId")
  val prisonCode: String,
)
