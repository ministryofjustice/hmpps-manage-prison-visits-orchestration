package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Alert dto response from alerts API")
data class AlertResponseDto(
  @param:Schema(description = "A summary of the alert", example = "2020-08-20", required = true)
  val alertCode: AlertCodeSummaryDto,

  @param:Schema(description = "Date of the alert, which might differ to the date it was created", required = true, example = "2019-08-20")
  val activeFrom: LocalDate,

  @param:Schema(description = "Date the alert expires", example = "2020-08-20")
  val activeTo: LocalDate? = null,

  @param:Schema(description = "The date and time the alert was created", example = "2021-09-27T14:19:25")
  val createdAt: LocalDateTime,

  @param:Schema(description = "The date and time the alert was last modified", required = false, example = "2019-08-20T14:19:25")
  val lastModifiedAt: LocalDateTime? = null,

  @param:Schema(description = "True / False based on alert status", example = "false", required = true)
  @JsonProperty("isActive")
  val active: Boolean,

  @param:Schema(description = "A comment / description of the alert", required = false)
  val description: String?,
)
