package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class NonAssociationChangedInfo(
  @field:NotBlank
  @param:JsonProperty("nsPrisonerNumber1")
  val prisonerNumber: String,
  @field:NotBlank
  @param:JsonProperty("nsPrisonerNumber2")
  val nonAssociationPrisonerNumber: String,
) : EventInfo
