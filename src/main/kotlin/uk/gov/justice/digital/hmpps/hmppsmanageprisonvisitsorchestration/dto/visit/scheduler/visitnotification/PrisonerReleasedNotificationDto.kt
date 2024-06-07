package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonerReleaseReasonType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo

data class PrisonerReleasedNotificationDto(
  @NotBlank
  val prisonerNumber: String,

  @NotBlank
  val prisonCode: String,

  @NotNull
  val reasonType: PrisonerReleaseReasonType,
) {

  constructor(info: PrisonerReleasedInfo) : this(
    info.prisonerNumber,
    info.prisonCode,
    PrisonerReleaseReasonType.valueOf(info.reasonType),
  )
}
