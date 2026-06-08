package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonAndSessionsExcludeDatesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonAndSessionsExcludeDatesService

const val PRISON_AND_SESSIONS_EXCLUDE_DATE_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_CONFIG_CONTROLLER_PATH/prison/{prisonCode}/prison-and-sessions/exclude-dates"
const val PRISON_AND_SESSIONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_PATH: String = "$PRISON_AND_SESSIONS_EXCLUDE_DATE_CONTROLLER_PATH/future"

@RestController
class PrisonAndSessionsExcludeDateController(
  private val prisonAndSessionsExcludeDatesService: PrisonAndSessionsExcludeDatesService,
) {
  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @GetMapping(PRISON_AND_SESSIONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_PATH)
  @Operation(
    summary = "Get any current or future dates excluded for visits for a prison and current or future dates excluded for individual sessions for the prison",
    description = "Get any current or future dates excluded for visits for a prison and current or future dates excluded for individual sessions for the prison",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prison and session exclude dates successfully returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonAndSessionsExcludeDatesDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to view exclude dates",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prison not found on visit-scheduler",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getFutureExcludeDatesForPrisonAndSessions(
    @Schema(description = "prison code", example = "HEI", required = true)
    @PathVariable
    prisonCode: String,
  ): PrisonAndSessionsExcludeDatesDto = prisonAndSessionsExcludeDatesService.getFuturePrisonAndSessionExcludeDates(prisonCode)
}
