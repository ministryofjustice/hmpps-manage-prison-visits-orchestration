package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PrisonerReleasedInfo(
  @field:NotBlank
  @param:JsonProperty("nomsNumber")
  val prisonerNumber: String,
  @field:NotBlank
  @param:JsonProperty("prisonId")
  val prisonCode: String,
  @field:NotBlank
  @param:JsonProperty("reason")
  val reasonType: String,
) : EventInfo
