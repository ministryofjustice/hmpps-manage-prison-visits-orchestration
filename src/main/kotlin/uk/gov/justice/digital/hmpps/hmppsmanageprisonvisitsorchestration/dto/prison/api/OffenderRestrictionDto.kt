package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Offender restriction")
data class OffenderRestrictionDto(
  @Schema(required = true, description = "restriction id")
  val restrictionId: Long,

  @Schema(description = "Restriction comment text")
  val comment: String? = null,

  @Schema(required = true, description = "code of restriction type")
  val restrictionType: String,

  @Schema(required = true, description = "description of restriction type")
  val restrictionTypeDescription: String,

  @Schema(required = true, description = "Date from which the restrictions applies", example = "1980-01-01")
  val startDate: LocalDate,

  @Schema(description = "Date restriction applies to, or indefinitely if null", example = "1980-01-01")
  val expiryDate: LocalDate? = null,

  @Schema(required = true, description = "true if restriction is within the start date and optional expiry date range")
  val active: Boolean,
)
