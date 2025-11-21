package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PrisonerAlertsUpdatedNotificationInfo(
  @field:NotBlank
  @param:JsonProperty("nomsNumber")
  val nomsNumber: String,

  @param:JsonProperty("alertsAdded")
  var alertsAdded: List<String>,

  @param:JsonProperty("alertsRemoved")
  var alertsRemoved: List<String>,
) : EventInfo
