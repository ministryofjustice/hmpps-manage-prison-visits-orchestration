package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Alert")
data class AlertDto(
  @Schema(required = true, description = "Alert Type", example = "X")
  @JsonProperty("alertType")
  val alertType: String,

  @Schema(required = true, description = "Alert Type Description", example = "Security")
  @JsonProperty("alertTypeDescription")
  val alertTypeDescription: String,

  @Schema(required = true, description = "Alert Code", example = "XER")
  @JsonProperty("alertCode")
  val alertCode: String,

  @Schema(required = true, description = "Alert Code Description", example = "Escape Risk")
  @JsonProperty("alertCodeDescription")
  val alertCodeDescription: String,

  @Schema(required = true, description = "Alert comments", example = "Profession lock pick.")
  @JsonProperty("comment")
  val comment: String,

  @Schema(
    required = true,
    description = "Date of the alert, which might differ to the date it was created",
    example = "2019-08-20"
  )
  @JsonProperty("dateCreated")
  val dateCreated: LocalDate,

  @Schema(description = "Date the alert expires", example = "2020-08-20")
  @JsonProperty("dateExpires")
  val dateExpires: LocalDate? = null,

  @Schema(required = true, description = "True / False based on presence of expiry date", example = "true")
  @JsonProperty("expired")
  val expired: Boolean = false,

  @Schema(required = true, description = "True / False based on alert status", example = "false")
  @JsonProperty("active")
  val active: Boolean = false,
)
