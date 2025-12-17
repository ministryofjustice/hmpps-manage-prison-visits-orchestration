package uk.gov.justice.digital.hmpps.orchestration.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class CurrentIncentive(
  @param:Schema(description = "Incentive level")
  val level: IncentiveLevel,
  @param:Schema(required = true, description = "Date time of the incentive", example = "2022-11-10T15:47:24")
  val dateTime: LocalDateTime,
  @param:Schema(required = true, description = "Schedule new review date", example = "2022-11-10")
  val nextReviewDate: LocalDate? = null,
)

data class IncentiveLevel(
  @param:Schema(description = "code", example = "STD")
  val code: String?,
  @param:Schema(required = true, description = "description", example = "Standard")
  val description: String,
)
