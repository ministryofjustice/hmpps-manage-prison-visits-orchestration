package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Alert dto response from alerts API")
data class AlertResponseDto(
  @Schema(description = "A summary of the alert", example = "2020-08-20", required = true)
  val alertCode: AlertCodeSummaryDto,

  @Schema(description = "Date of the alert, which might differ to the date it was created", required = true, example = "2019-08-20")
  val createdAt: LocalDate,

  @Schema(description = "Date the alert expires", example = "2020-08-20")
  val activeTo: LocalDate? = null,

  @Schema(description = "True / False based on alert status", example = "false", required = true)
  val active: Boolean,

  @Schema(description = "A comment / description of the alert", required = false)
  val description: String,
)
