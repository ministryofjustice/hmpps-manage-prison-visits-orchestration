package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerAlertsUpdatedNotificationInfo

data class PrisonerAlertsAddedNotificationDto(
  @NotBlank
  val prisonerNumber: String,

  @JsonInclude(JsonInclude.Include.NON_NULL)
  val alertsAdded: List<String>,

  @JsonInclude(JsonInclude.Include.NON_NULL)
  val alertsRemoved: List<String>,

  @NotBlank
  val description: String,
) {

  constructor(info: PrisonerAlertsUpdatedNotificationInfo, description: String) : this(
    prisonerNumber = info.nomsNumber,
    alertsAdded = info.alertsAdded,
    alertsRemoved = info.alertsRemoved,
    description = description,
  )
}
