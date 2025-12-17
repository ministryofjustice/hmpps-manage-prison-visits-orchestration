package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.enums.VisitOrderHistoryAttributeType

data class VisitOrderHistoryAttributesDto(
  @param:Schema(description = "Visit order history attribute type", example = "VISIT_REFERENCE", required = true)
  val attributeType: VisitOrderHistoryAttributeType,

  @param:Schema(description = "Visit order history attribute value", required = true)
  val attributeValue: String,
)
