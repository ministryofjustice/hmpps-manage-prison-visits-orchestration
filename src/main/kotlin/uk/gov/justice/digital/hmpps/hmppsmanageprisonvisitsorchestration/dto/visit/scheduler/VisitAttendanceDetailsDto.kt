package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(description = "Additional visit attendance details")
data class VisitAttendanceDetailsDto(
  @param:Schema(description = "Search carried out in connection with the visit")
  val search: VisitSearchDto? = null,
  @param:Schema(description = "Actual local start time", type = "string", example = "13:45:00")
  val actualStartTime: LocalTime? = null,
  @param:Schema(description = "Actual local end time", type = "string", example = "14:30:00")
  val actualEndTime: LocalTime? = null,
)

@Schema(description = "Search details for a visit")
data class VisitSearchDto(
  @param:Schema(description = "Who was searched", example = "PRISONER")
  val subject: VisitSearchSubject,
  @param:Schema(description = "Type of search, if recorded", example = "FULL_SEARCH")
  val type: VisitSearchType? = null,
)

enum class VisitSearchSubject {
  PRISONER,
  VISITORS,
}

enum class VisitSearchType {
  FULL_SEARCH,
  RUBDOWN_LEVEL_A,
  RUBDOWN_LEVEL_B,
}
