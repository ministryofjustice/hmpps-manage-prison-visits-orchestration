package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.EventAuditAttributeTypes

data class EventAuditAttributeDto(
  @param:Schema(description = "Name of the attribute associated with the event audit", example = "AUTO_REJECTION_REASON", required = true)
  @field:NotNull
  val attributeName: EventAuditAttributeTypes,

  @param:Schema(description = "Value of the attribute associated with the event audit", example = "Transfer", required = true)
  @field:NotNull
  val attributeValue: String,
)
