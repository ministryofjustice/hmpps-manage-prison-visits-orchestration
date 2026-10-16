package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.NotEmpty
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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.sessions.VisitSessionsAndScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerSessionsService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.validation.NullableNotEmpty
import java.time.LocalDate
import java.time.LocalTime

const val GET_VISIT_SESSIONS = "/visit-sessions"
const val GET_VISIT_SESSIONS_AND_SCHEDULE = "/visit-sessions-and-schedule"
const val GET_VISIT_SESSIONS_AVAILABLE = "$GET_VISIT_SESSIONS/available"
const val GET_VISIT_SESSIONS_AVAILABLE_PUBLIC = "$GET_VISIT_SESSIONS/public/available"
const val GET_VISIT_SESSIONS_AVAILABLE_RESTRICTION = "$GET_VISIT_SESSIONS_AVAILABLE/restriction"
const val GET_VISIT_SESSIONS_CAPACITY = "$GET_VISIT_SESSIONS/capacity"

const val GET_VISIT_SESSION = "$GET_VISIT_SESSIONS/session"

const val GET_VISIT_SESSIONS_SCHEDULE = "$GET_VISIT_SESSIONS/schedule"

@RestController
class OrchestrationSessionsController(private val visitSchedulerSessionsService: VisitSchedulerSessionsService) {
  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSIONS_AND_SCHEDULE)
  @Operation(
    summary = "Returns all visit sessions and prisoner schedule for a specified prisoner for STAFF client",
    description = "Retrieve all visit sessions and schedule for a specified prisoner for STAFF client",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit sessions and schedule information returned for a prisoner",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get visit sessions and schedule information for a prisoner ",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitSessionsAndScheduleForStaff(
    @RequestParam(value = "prisonId", required = true)
    @Parameter(
      description = "Query by NOMIS Prison Identifier",
      example = "MDI",
    )
    prisonCode: String,
    @RequestParam(value = "prisonerId", required = true)
    @Parameter(
      description = "Filter results by prisoner id",
      example = "A12345DC",
    )
    prisonerId: String,
    @RequestParam(value = "min", required = false)
    @Parameter(
      description = "Override the default minimum number of days notice from the current date",
      example = "2",
    )
    min: Int?,
    @RequestParam(value = "username", required = false)
    @Parameter(
      description = "Username for the user making the request. Used to exclude user's pending applications from session capacity count. Optional, ignored if not passed in.",
      example = "user-1",
    )
    username: String? = null,
    @RequestParam(value = "youngestVisitorAge", required = false)
    @Parameter(description = "Age of the youngest visitor", example = "18", required = false)
    youngestVisitorAge: Int? = null,
  ): VisitSessionsAndScheduleDto = visitSchedulerSessionsService.getVisitSessionsAndScheduleForStaff(
    prisonCode,
    prisonerId,
    min,
    username,
    youngestVisitorAge,
  )

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSIONS_AVAILABLE)
  @Operation(
    summary = "Returns only available visit sessions for a specified prisoner by restriction and within the reservable time period for PVB",
    description = "Returns only available visit sessions for a specified prisoner by restriction and within the reservable time period for PVB. Always checks prisoner restrictions and higher priority appointments. Visitor restrictions, review flags and caller overrides are not applied. Uses the same prison-configured booking window as the PUBLIC service endpoint, with full working days of minimum notice excluding weekends and England and Wales bank holidays.",
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
  fun getAvailableVisitSessionsForPvb(
    @RequestParam(value = "prisonId", required = true)
    @Parameter(description = "Query by NOMIS Prison Identifier", example = "MDI", required = true)
    prisonCode: String,
    @RequestParam(value = "prisonerId", required = true)
    @Parameter(description = "Filter results by prisoner id", example = "A12345DC", required = true)
    prisonerId: String,
  ): List<AvailableVisitSessionDto> = visitSchedulerSessionsService.getAvailableVisitSessionsForPvb(
    prisonCode = prisonCode,
    prisonerId = prisonerId,
  )

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISIT_SESSIONS_AVAILABLE_PUBLIC)
  @Operation(
    summary = "Returns available visit sessions for a prisoner and visitors, marking sessions that require review.",
    description = "Returns available visit sessions after the prison's minimum notice period in full working days, excluding weekends and England and Wales bank holidays. Visits can start on the following calendar day. The same notice period applies to auto-approved sessions and sessions marked for review. Marks sessions for review if prisoner alerts / restrictions or visitor restrictions are found. Used by Visits Public only, not PVB",
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
  fun getAvailableVisitSessionsForPublic(
    @RequestParam(value = "prisonId", required = true)
    @Parameter(description = "Query by NOMIS Prison Identifier", example = "MDI", required = true)
    prisonCode: String,
    @RequestParam(value = "prisonerId", required = true)
    @Parameter(description = "Filter results by prisoner id", example = "A12345DC", required = true)
    prisonerId: String,
    @RequestParam(value = "visitors", required = true)
    @Parameter(
      description = "comma separated list of visitors who require visit sessions",
      example = "4729510,4729220",
    )
    @NotEmpty(message = "An empty visitors list is not allowed")
    visitors: List<Long>,
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
    @RequestParam(value = "youngestVisitorAge", required = false)
    @Parameter(description = "Age of the youngest visitor", example = "18", required = false)
    youngestVisitorAge: Int? = null,
  ): List<AvailableVisitSessionDto> = visitSchedulerSessionsService.getAvailableVisitSessionsForPublic(
    prisonCode = prisonCode,
    prisonerId = prisonerId,
    visitors = visitors,
    excludedApplicationReference = excludedApplicationReference,
    username = username,
    youngestVisitorAge = youngestVisitorAge,
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
    @RequestParam(value = "includeExcludedSessions", required = false)
    @Parameter(description = "Flag to indicate if excluded sessions should be part of the response. If false, any excluded sessions will not be returned. Defaults to false.", example = "false", required = false)
    includeExcludedSessions: Boolean?,
  ): List<SessionScheduleDto>? = visitSchedulerSessionsService.getSessionSchedule(prisonCode, sessionDate, includeExcludedSessions ?: false)
}
