package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo

data class PrisonerReceivedNotificationDto(
  @NotBlank
  @JsonProperty("nomsNumber")
  val prisonerNumber: String,
) {

  constructor(info: PrisonerReceivedInfo) : this(
    info.prisonerNumber,
  )
}
