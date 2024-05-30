package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PrisonerAlertsUpdatedNotificationInfo(
  @NotBlank
  @JsonProperty("nomsNumber")
  val nomsNumber: String,
  @NotBlank
  @JsonProperty("alertsAdded")
  val alertsAdded: List<String>,
)
