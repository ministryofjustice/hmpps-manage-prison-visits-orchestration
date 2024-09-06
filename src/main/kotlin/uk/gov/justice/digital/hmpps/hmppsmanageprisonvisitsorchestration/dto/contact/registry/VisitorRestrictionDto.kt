package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Visitor restriction")
data class VisitorRestrictionDto(
  @Schema(description = "Restriction Type", example = "BAN", required = true)
  val restrictionType: VisitorRestrictionType,
  @Schema(description = "Restriction Expiry", example = "2029-12-31", required = false)
  val expiryDate: LocalDate? = null,
)
