package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonerReceivedReasonType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo

data class PrisonerReceivedNotificationDto(
  @NotBlank
  val prisonerNumber: String,

  @NotNull
  val prisonCode: String,

  @NotNull
  val reason: PrisonerReceivedReasonType,
) {

  constructor(info: PrisonerReceivedInfo) : this (
    prisonerNumber = info.prisonerNumber,
    prisonCode = info.prisonCode,
    reason = info.reason,
  )
}
