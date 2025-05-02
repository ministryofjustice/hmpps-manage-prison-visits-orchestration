package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "AlertDto returned from orchestration, made of fields from AlertResponseDto from Alerts API call")
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
  val startDate: LocalDate,

  @Schema(description = "Date the alert expires", example = "2020-08-20")
  val expiryDate: LocalDate? = null,

  @Schema(description = "Date the alert was last updated.", example = "2020-08-20")
  val dateUpdated: LocalDate? = null,

  @Schema(required = true, description = "True / False based on alert status", example = "false")
  val active: Boolean = false,
) {
  // Secondary constructor that initializes AlertDto from AlertResponseDto
  constructor(alertResponseDto: AlertResponseDto) : this(
    alertType = alertResponseDto.alertCode.alertTypeCode,
    alertTypeDescription = alertResponseDto.alertCode.alertTypeDescription,
    alertCode = alertResponseDto.alertCode.code,
    alertCodeDescription = alertResponseDto.alertCode.description,
    comment = alertResponseDto.description,
    startDate = alertResponseDto.createdAt,
    expiryDate = alertResponseDto.activeTo,
    active = alertResponseDto.active,
    dateUpdated = alertResponseDto.lastModifiedAt,
  )
}
