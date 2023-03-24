package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Visitor")
class VisitorDto(
  @Schema(description = "Person ID (nomis) of the visitor", example = "1234", required = true)
  @field:NotNull
  val nomisPersonId: Long,

  @Schema(description = "true if visitor is the contact for the visit otherwise false", example = "true", required = false)
  val visitContact: Boolean?,
)
