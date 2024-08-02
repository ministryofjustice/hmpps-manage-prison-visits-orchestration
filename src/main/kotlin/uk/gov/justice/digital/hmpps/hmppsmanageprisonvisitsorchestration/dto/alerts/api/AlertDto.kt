package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Alert")
data class AlertDto(
  @Schema(required = true, description = "Alert Type", example = "X")
  val alertType: String,

  @Schema(required = true, description = "Alert Type Description", example = "Security")
  val alertTypeDescription: String,

  @Schema(required = true, description = "Alert Code", example = "XER")
  val alertCode: String,

  @Schema(required = true, description = "Alert Code Description", example = "Escape Risk")
  val alertCodeDescription: String,

  @Schema(required = false, description = "Alert comments", example = "Profession lock pick.")
  val comment: String? = null,

  @Schema(
    required = true,
    description = "Date of the alert, which might differ to the date it was created",
    example = "2019-08-20",
  )
  val dateCreated: LocalDate,

  @Schema(description = "Date the alert expires", example = "2020-08-20")
  val dateExpires: LocalDate? = null,

  @Schema(required = true, description = "True / False based on presence of expiry date", example = "true")
  val expired: Boolean = false,

  @Schema(required = true, description = "True / False based on alert status", example = "false")
  val active: Boolean = false,
)
