package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ReleaseReasonType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo

data class PrisonerReceivedNotificationDto(
  @NotBlank
  val prisonerNumber: String,

  @NotBlank
  val reason: ReleaseReasonType,

  @NotBlank
  val prisonCode: String,
) {

  constructor(info: PrisonerReceivedInfo) : this (
    prisonerNumber = info.prisonerNumber,
    reason = info.reason,
    prisonCode = info.prisonCode,
  )
}
