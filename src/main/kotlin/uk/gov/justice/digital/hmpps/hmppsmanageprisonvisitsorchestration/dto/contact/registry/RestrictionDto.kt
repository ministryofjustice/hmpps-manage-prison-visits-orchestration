package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
data class RestrictionDto(
  @Schema(description = "Restriction Id", example = "123", required = true) val restrictionId: Int,
  @Schema(description = "Restriction Type Code", example = "123", required = true) val restrictionType: String,
  @Schema(description = "Description of Restriction Type", example = "123", required = true) val restrictionTypeDescription: String,
  @Schema(description = "Date from which the restriction applies", example = "2000-10-31", required = true) val startDate: LocalDate,
  @Schema(description = "Restriction Expiry", example = "2000-10-31", required = false) val expiryDate: LocalDate? = null,
  @Schema(description = "True if applied globally to the contact or False if applied in the context of a visit", required = true) val globalRestriction: Boolean,
  @Schema(description = "Additional Information", example = "This is a comment text", required = false) val comment: String? = null,
)
