package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A summary of the alert")
data class AlertCodeSummaryDto(
  @Schema(required = true, description = "Alert Type", example = "X")
  val alertTypeCode: String,

  @Schema(required = true, description = "Alert Type Description", example = "Security")
  val alertTypeDescription: String,

  @Schema(required = true, description = "Alert Code", example = "XER")
  val code: String,

  @Schema(required = true, description = "Alert Code Description", example = "Escape Risk")
  val description: String,
)
