package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerAlertUpsertedInfo

data class PrisonerAlertUpsertedNotificationDto(
  @field:NotBlank
  val prisonerNumber: String,

  @field:NotBlank
  val alertCode: String,

  @field:NotBlank
  val alertUuid: String,

  @field:NotBlank
  val description: String,
) {
  constructor(info: PrisonerAlertUpsertedInfo, description: String) : this(
    prisonerNumber = requireNotNull(info.prisonerNumber) { "Prisoner number is required" },
    alertCode = info.alertCode,
    alertUuid = info.alertUuid,
    description = description,
  )
}
