package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.enums.PrisonerReleaseReasonType
import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo

data class PrisonerReleasedNotificationDto(
  @field:NotBlank
  val prisonerNumber: String,

  @field:NotBlank
  val prisonCode: String,

  @field:NotNull
  val reasonType: PrisonerReleaseReasonType,
) {

  constructor(info: PrisonerReleasedInfo) : this(
    info.prisonerNumber,
    info.prisonCode,
    PrisonerReleaseReasonType.valueOf(info.reasonType),
  )
}
