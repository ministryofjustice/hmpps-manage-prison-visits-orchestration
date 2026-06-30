package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerMergedInfo

data class PrisonerMergedNotificationDto(
  @field:NotBlank
  val oldPrisonerNumber: String,

  @field:NotBlank
  val newPrisonerNumber: String,
) {

  constructor(info: PrisonerMergedInfo) : this(
    oldPrisonerNumber = info.oldPrisonerNumber,
    newPrisonerNumber = info.newPrisonerNumber,
  )
}
