package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorApprovedUnapprovedInfo

data class VisitorApprovedUnapprovedNotificationDto(
  @Schema(description = "Prisoner Number", example = "AF34567G", required = true)
  @field:NotBlank
  val prisonerNumber: String,

  @Schema(description = "Visitor ID", example = "1246424", required = true)
  @NotBlank
  val visitorId: String,
) {

  constructor(info: VisitorApprovedUnapprovedInfo) : this(
    info.prisonerNumber,
    info.visitorId,
  )
}
