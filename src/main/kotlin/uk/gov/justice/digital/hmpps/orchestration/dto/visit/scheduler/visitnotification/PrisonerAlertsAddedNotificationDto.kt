package uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.visitnotification

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.events.additionalinfo.PrisonerAlertsUpdatedNotificationInfo

data class PrisonerAlertsAddedNotificationDto(
  @field:NotBlank
  val prisonerNumber: String,

  @param:JsonInclude(JsonInclude.Include.NON_NULL)
  val alertsAdded: List<String>,

  @param:JsonInclude(JsonInclude.Include.NON_NULL)
  val alertsRemoved: List<String>,

  @param:JsonInclude(JsonInclude.Include.NON_NULL)
  val activeAlerts: List<String>,

  @field:NotBlank
  val description: String,
) {

  constructor(info: PrisonerAlertsUpdatedNotificationInfo, activeAlerts: List<String>, description: String) : this(
    prisonerNumber = info.nomsNumber,
    alertsAdded = info.alertsAdded,
    alertsRemoved = info.alertsRemoved,
    activeAlerts = activeAlerts,
    description = description,
  )
}
