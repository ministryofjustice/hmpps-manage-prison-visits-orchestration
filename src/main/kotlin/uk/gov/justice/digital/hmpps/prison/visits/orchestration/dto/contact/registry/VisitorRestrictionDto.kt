package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Visitor restriction")
data class VisitorRestrictionDto(
  @param:Schema(description = "Restriction Type", example = "BAN", required = true)
  val restrictionType: VisitorRestrictionType,
  @param:Schema(description = "Restriction Expiry", example = "2029-12-31", required = false)
  val expiryDate: LocalDate? = null,
)
