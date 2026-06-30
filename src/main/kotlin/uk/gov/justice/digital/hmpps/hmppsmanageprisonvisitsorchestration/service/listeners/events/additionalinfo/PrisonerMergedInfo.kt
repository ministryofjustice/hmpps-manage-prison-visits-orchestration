package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PrisonerMergedInfo(
  @field:NotBlank
  @param:JsonProperty("nomsNumber")
  val newPrisonerNumber: String,
  @field:NotBlank
  @param:JsonProperty("removedNomsNumber")
  val oldPrisonerNumber: String,
) : EventInfo
