package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo

data class PrisonerReleasedNotificationDto(
  @NotBlank
  val prisonerNumber: String,
) {

  constructor(info: PrisonerReleasedInfo) : this(
    info.prisonerNumber,
  )
}
