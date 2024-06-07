package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonerReceivedReasonType

data class PrisonerReceivedInfo(
  @NotBlank
  @JsonProperty("nomsNumber")
  val prisonerNumber: String,

  @NotNull
  @JsonProperty("reason")
  val reason: PrisonerReceivedReasonType,
)
