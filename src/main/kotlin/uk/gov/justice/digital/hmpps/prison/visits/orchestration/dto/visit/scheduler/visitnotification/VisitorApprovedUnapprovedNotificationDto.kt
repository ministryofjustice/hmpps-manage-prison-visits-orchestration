package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.visitnotification

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo.VisitorApprovedUnapprovedInfo

data class VisitorApprovedUnapprovedNotificationDto(
  @param:Schema(description = "Prisoner Number", example = "AF34567G", required = true)
  @field:NotBlank
  val prisonerNumber: String,

  @param:Schema(description = "Visitor ID", example = "1246424", required = true)
  @field:NotBlank
  val visitorId: String,
) {

  constructor(info: VisitorApprovedUnapprovedInfo) : this(
    info.prisonerNumber,
    info.visitorId,
  )
}
