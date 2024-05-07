package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Has closed restriction")
data class HasClosedRestrictionDto(
  @Schema(description = "has closed restriction")
  val value: Boolean,
)
