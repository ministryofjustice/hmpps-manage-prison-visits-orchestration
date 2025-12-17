package uk.gov.justice.digital.hmpps.orchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class VisitorApprovedUnapprovedInfo(
  @field:NotBlank
  @param:JsonProperty("nomsNumber")
  val prisonerNumber: String,

  @field:NotBlank
  @param:JsonProperty("personId")
  val visitorId: String,
) : EventInfo
