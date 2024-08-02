package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Alert")
data class AlertResponseDto(
  @Schema(description = "Date the alert expires", example = "2020-08-20", required = true)
  val alertCode: AlertCodeSummaryDto,

  @Schema(
    required = true,
    description = "Date of the alert, which might differ to the date it was created",
    example = "2019-08-20",
  )
  @JsonProperty("createdAt")
  val dateCreated: LocalDate,

  @Schema(description = "Date the alert expires", example = "2020-08-20")
  @JsonProperty("activeTo")
  val dateExpires: LocalDate? = null,

  @Schema(required = true, description = "True / False based on alert status", example = "false")
  @JsonProperty("isActive")
  val active: Boolean = false,
)
