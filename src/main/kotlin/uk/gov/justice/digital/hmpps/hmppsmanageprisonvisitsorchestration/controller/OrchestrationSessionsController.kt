package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.validation.NullableNotEmpty
import java.time.LocalDate
import java.time.LocalTime

@RestController
class OrchestrationSessionsController(private val visitSchedulerService: VisitSchedulerService) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping("/visit-sessions")
  @Operation(
    summary = "Returns all visit sessions which are within the reservable time period - whether or not they are full",
    description = "Retrieve all visits for a specified prisoner",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit session information returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get visit sessions ",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitSessions(
    @RequestParam(value = "prisonId", required = true)
    @Parameter(
      description = "Query by NOMIS Prison Identifier",
      example = "MDI",
    )
    prisonCode: String,
    @RequestParam(value = "prisonerId", required = false)
    @Parameter(
      description = "Filter results by prisoner id",
      example = "A12345DC",
    )
    prisonerId: String?,
    @RequestParam(value = "min", required = false)
    @Parameter(
      description = "Override the default minimum number of days notice from the current date",
      example = "2",
    )
    min: Int?,
    @RequestParam(value = "max", required = false)
    @Parameter(
      description = "Override the default maximum number of days to book-ahead from the current date",
      example = "28",
    )
    max: Int?,
  ): List<VisitSessionDto>? =
    visitSchedulerService.getVisitSessions(prisonCode, prisonerId, min, max)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping("/visit-sessions/available")
  @Operation(
    summary = "Returns only available visit sessions for a specified prisoner by restriction and within the reservable time period",
    description = "Returns only available visit sessions for a specified prisoner by restriction and within the reservable time period",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit session information returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get visit sessions ",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAvailableVisitSessions(
    @RequestParam(value = "prisonId", required = true)
    @Parameter(description = "Query by NOMIS Prison Identifier", example = "MDI", required = true)
    prisonCode: String,
    @RequestParam(value = "prisonerId", required = true)
    @Parameter(description = "Filter results by prisoner id", example = "A12345DC", required = true)
    prisonerId: String,
    @RequestParam(value = "sessionRestriction", required = false)
    @Parameter(description = "Filter sessions by session restriction - OPEN or CLOSED, if prisoner has CLOSED it will use that", example = "CLOSED", required = false)
    sessionRestriction: SessionRestriction = SessionRestriction.OPEN,
    @RequestParam(value = "visitors", required = false)
    @Parameter(
      description = "List of visitors who require visit sessions",
      example = "4729510,4729220",
    )
    @NullableNotEmpty(message = "An empty visitors list is not allowed")
    visitors: List<Long>? = null,
  ): List<AvailableVisitSessionDto> =
    visitSchedulerService.getAvailableVisitSessions(prisonCode, prisonerId, sessionRestriction, visitors)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping("/visit-sessions/capacity")
  @Operation(
    summary = "Returns the VSIP session capacity for the given sessions",
    description = "Returns the VSIP session capacity for the given sessions",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the session capacity for the given sessions",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request ",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Capacity not found ",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSessionCapacity(
    @RequestParam(value = "prisonId", required = true)
    @Parameter(
      description = "Query by NOMIS Prison Identifier",
      example = "CLI",
    )
    prisonCode: String,
    @RequestParam(value = "sessionDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Session date",
      example = "2020-11-01",
    )
    sessionDate: LocalDate,
    @RequestParam(value = "sessionStartTime", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Parameter(
      description = "Session start time",
      example = "13:30:00",
    )
    sessionStartTime: LocalTime,
    @RequestParam(value = "sessionEndTime", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Parameter(
      description = "Session end time",
      example = "14:30:00",
    )
    sessionEndTime: LocalTime,
  ): SessionCapacityDto? = visitSchedulerService.getSessionCapacity(prisonCode, sessionDate, sessionStartTime, sessionEndTime)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping("/visit-sessions/schedule")
  @Operation(
    summary = "Returns session scheduled for given prison and date",
    description = "Returns session scheduled for given prison and date",

    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Session templates returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to view session templates",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSessionSchedule(
    @RequestParam(value = "prisonId", required = true)
    @Parameter(
      description = "Query by NOMIS Prison Identifier",
      example = "CLI",
    )
    prisonCode: String,
    @RequestParam(value = "date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Session date",
      example = "2023-01-31",
    )
    sessionDate: LocalDate,
  ): List<SessionScheduleDto>? = visitSchedulerService.getSessionSchedule(prisonCode, sessionDate)
}
