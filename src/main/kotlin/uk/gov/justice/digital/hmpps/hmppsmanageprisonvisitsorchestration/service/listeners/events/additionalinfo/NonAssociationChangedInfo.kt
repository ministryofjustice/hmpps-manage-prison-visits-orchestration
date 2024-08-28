package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class NonAssociationChangedInfo(
  @NotBlank
  @JsonProperty("nsPrisonerNumber1")
  val prisonerNumber: String,
  @NotBlank
  @JsonProperty("nsPrisonerNumber2")
  val nonAssociationPrisonerNumber: String,
) : EventInfo
