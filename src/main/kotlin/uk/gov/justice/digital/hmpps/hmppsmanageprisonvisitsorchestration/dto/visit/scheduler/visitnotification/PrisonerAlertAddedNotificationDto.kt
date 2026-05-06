package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerAlertAddedInfo

data class PrisonerAlertAddedNotificationDto(
  @field:NotBlank
  val prisonerNumber: String,

  @field:NotBlank
  val alertCode: String,

  @field:NotBlank
  val alertUUID: String,

  @field:NotBlank
  val description: String,
) {
  constructor(info: PrisonerAlertAddedInfo, description: String) : this(
    prisonerNumber = requireNotNull(info.prisonerNumber) { "Prisoner number is required" },
    alertCode = info.alertCode,
    alertUUID = info.alertUUID,
    description = description,
  )
}
