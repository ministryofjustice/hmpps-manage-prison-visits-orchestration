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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.AvailableVisitSessionRestrictionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionCapacityDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SessionScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSessionDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.SessionRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerSessionsService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.validation.NullableNotEmpty
import java.time.LocalDate
import java.time.LocalTime

const val GET_VISIT_SESSIONS = "/visit-sessions"
const val GET_VISIT_SESSIONS_AVAILABLE = "$GET_VISIT_SESSIONS/available"
const val GET_VISIT_SESSIONS_AVAILABLE_V2 = "$GET_VISIT_SESSIONS/available/v2"
const val GET_VISIT_SESSIONS_AVAILABLE_RESTRICTION = "$GET_VISIT_SESSIONS_AVAILABLE/restriction"
const val GET_VISIT_SESSIONS_CAPACITY = "$GET_VISIT_SESSIONS/capacity"

const val GET_VISIT_SESSION = "$GET_VISIT_SESSIONS/session"

const val GET_VISIT_SESSIONS_SCHEDULE = "$GET_VISIT_SESSIONS/schedule"

@RestController
class OrchestrationSessionsController(private val visitSchedulerSessionsService: VisitSchedulerSessionsService) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSIONS)
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
    @RequestParam(value = "username", required = false)
    @Parameter(
      description = "Username for the user making the request. Used to exclude user's pending applications from session capacity count. Optional, ignored if not passed in.",
      example = "user-1",
    )
    username: String? = null,
    @RequestParam
    @Parameter(description = "user type for the session", example = "STAFF", required = false)
    userType: UserType = UserType.STAFF,
  ): List<VisitSessionDto>? = visitSchedulerSessionsService.getVisitSessions(prisonCode, prisonerId, min, max, username, userType)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSIONS_AVAILABLE)
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
    sessionRestriction: SessionRestriction? = null,
    @RequestParam(value = "visitors", required = false)
    @Parameter(
      description = "List of visitors who require visit sessions",
      example = "4729510,4729220",
    )
    @NullableNotEmpty(message = "An empty visitors list is not allowed")
    visitors: List<Long>? = null,
    @RequestParam(value = "withAppointmentsCheck", required = false)
    @Parameter(
      description = "Defaults to true if not passed. If true, will not return visit times that clash with higher priority legal or medical appointments.",
    )
    withAppointmentsCheck: Boolean? = true,
    // TODO - to be removed as PVB does not use this parameter
    @RequestParam(value = "excludedApplicationReference", required = false)
    @Parameter(
      description = "The current application reference to be excluded from capacity count and double booking",
      example = "dfs-wjs-eqr",
    )
    excludedApplicationReference: String? = null,
    @RequestParam(value = "pvbAdvanceFromDateByDays", required = false)
    @Parameter(description = "For PVB only. Allows service to advance the opening session slot booking window by n days on top of any other overrides. Defaults to 0 if not passed.")
    pvbAdvanceFromDateByDays: Int? = 0,
    @RequestParam(value = "fromDateOverride", required = false)
    @Parameter(description = "minimum override in days for opening session slot booking window, E.g. 2 will set min booking window to today + 2 days")
    fromDateOverride: Int? = null,
    @RequestParam(value = "toDateOverride", required = false)
    @Parameter(description = "maximum override in days for closing session slot booking window, E.g. 28 will set max booking window to today + 28 days")
    toDateOverride: Int? = null,
    @RequestParam(value = "username", required = false)
    @Parameter(
      description = "Username for the user making the request. Used to exclude user's pending applications from session capacity count. Optional, ignored if not passed in.",
      example = "user-1",
    )
    username: String? = null,
    @RequestParam
    @Parameter(description = "user type for the session", example = "PUBLIC", required = false)
    userType: UserType = UserType.PUBLIC,
  ): List<AvailableVisitSessionDto> = visitSchedulerSessionsService.getAvailableVisitSessions(
    prisonCode = prisonCode,
    prisonerId = prisonerId,
    requestedSessionRestriction = sessionRestriction,
    withAppointmentsCheck = withAppointmentsCheck ?: true,
    pvbAdvanceFromDateByDays = pvbAdvanceFromDateByDays ?: 0,
    visitors = visitors,
    fromDateOverride = fromDateOverride,
    toDateOverride = toDateOverride,
    username = username,
    userType = userType,
    // TODO - to be removed as PVB does not use this parameter
    excludedApplicationReference = excludedApplicationReference,
  )

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSIONS_AVAILABLE_V2)
  @Operation(
    summary = "Returns available visit sessions for a specified prisoner and visitors combination for the date range passed in",
    description = "Returns available visit sessions for a specified prisoner and visitors combination for the date range passed in. Used by Visits Public only, not PVB",
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
    @RequestParam(value = "visitors", required = false)
    @Parameter(
      description = "List of visitors who require visit sessions",
      example = "4729510,4729220",
    )
    @NullableNotEmpty(message = "An empty visitors list is not allowed")
    visitors: List<Long>? = null,
    @RequestParam(value = "excludedApplicationReference", required = false)
    @Parameter(
      description = "The current application reference to be excluded from capacity count and double booking",
      example = "dfs-wjs-eqr",
    )
    excludedApplicationReference: String? = null,
    @RequestParam(value = "username", required = false)
    @Parameter(
      description = "Username for the user making the request. Used to exclude user's pending applications from session capacity count. Optional, ignored if not passed in.",
      example = "user-1",
    )
    username: String? = null,
    @RequestParam
    @Parameter(description = "user type for the session", example = "PUBLIC", required = false)
    userType: UserType = UserType.PUBLIC,
  ): List<AvailableVisitSessionDto> = visitSchedulerSessionsService.getAvailableVisitSessions(
    prisonCode = prisonCode,
    prisonerId = prisonerId,
    visitors = visitors,
    excludedApplicationReference = excludedApplicationReference,
    username = username,
    userType = userType,
  )

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSIONS_AVAILABLE_RESTRICTION)
  @Operation(
    summary = "Returns the restriction type of available sessions",
    description = "Returns the restriction of available sessions given a prisoner and optionally a list of visitors [OPEN / CLOSED]",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Available visit session restriction returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get available visit session restriction",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSessionRestrictionType(
    @RequestParam(value = "prisonerId", required = true)
    @Parameter(description = "Filter results by prisoner id", example = "A12345DC", required = true)
    prisonerId: String,
    @RequestParam(value = "visitors", required = false)
    @Parameter(
      description = "List of visitors who require visit sessions",
      example = "4729510,4729220",
    )
    @NullableNotEmpty(message = "An empty visitors list is not allowed")
    visitors: List<Long>? = null,
  ): AvailableVisitSessionRestrictionDto = visitSchedulerSessionsService.getAvailableVisitSessionsRestriction(
    prisonerId = prisonerId,
    visitors = visitors,
  )

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSIONS_CAPACITY)
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
  ): SessionCapacityDto? = visitSchedulerSessionsService.getSessionCapacity(prisonCode, sessionDate, sessionStartTime, sessionEndTime)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSION)
  @Operation(
    summary = "Returns a single VSIP session",
    description = "Returns a single VSIP session",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the session was found and returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Session not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitSession(
    @RequestParam(value = "prisonCode", required = true)
    @Parameter(
      description = "Prison code",
      example = "MDI",
    )
    prisonCode: String,
    @RequestParam(value = "sessionDate", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Session date",
      example = "2020-11-01",
    )
    sessionDate: LocalDate,
    @RequestParam(value = "sessionTemplateReference", required = true)
    @Parameter(
      description = "Session template reference",
      example = "xye-fjc-abc",
    )
    sessionTemplateReference: String,
  ): VisitSessionDto? = visitSchedulerSessionsService.getSession(prisonCode, sessionDate, sessionTemplateReference)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSIONS_SCHEDULE)
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
  ): List<SessionScheduleDto>? = visitSchedulerSessionsService.getSessionSchedule(prisonCode, sessionDate)
}
