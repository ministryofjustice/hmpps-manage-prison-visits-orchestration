package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.PrisonerReceivedReasonType
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo

data class PrisonerReceivedNotificationDto(
  @field:NotBlank
  val prisonerNumber: String,

  @field:NotNull
  val prisonCode: String,

  @field:NotNull
  val reason: PrisonerReceivedReasonType,
) {

  constructor(info: PrisonerReceivedInfo) : this (
    prisonerNumber = info.prisonerNumber,
    prisonCode = info.prisonCode,
    reason = info.reason,
  )
}
