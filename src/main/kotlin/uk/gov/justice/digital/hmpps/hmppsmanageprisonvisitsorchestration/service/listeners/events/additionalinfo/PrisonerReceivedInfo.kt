package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.CurrentLocation
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.CurrentPrisonStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ReleaseReasonType

data class PrisonerReceivedInfo(
  @NotBlank
  @JsonProperty("nomsNumber")
  val prisonerNumber: String,

  @NotBlank
  @JsonProperty("reason")
  val reason: ReleaseReasonType,

  @NotBlank
  @JsonProperty("detail")
  val detail: String,

  @NotBlank
  @JsonProperty("currentLocation")
  val currentLocation: CurrentLocation,

  @NotBlank
  @JsonProperty("currentPrisonStatus")
  val currentPrisonStatus: CurrentPrisonStatus,

  @NotBlank
  @JsonProperty("prisonId")
  val prisonCode: String,

  @NotBlank
  @JsonProperty("nomisMovementReasonCode")
  val nomisMovementReasonCode: String,
)
