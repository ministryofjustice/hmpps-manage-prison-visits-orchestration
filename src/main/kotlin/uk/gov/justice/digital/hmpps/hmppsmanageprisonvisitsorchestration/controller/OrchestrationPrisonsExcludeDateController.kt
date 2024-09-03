package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonService

const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_CONFIG_CONTROLLER_PATH/prison/{prisonCode}/exclude-date"
const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_ADD_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH/add"
const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_REMOVE_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH/remove"
const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH/future"
const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_GET_PAST_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH/past"

@RestController
class OrchestrationPrisonsExcludeDateController(
  private val prisonService: PrisonService,
) {
  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(ORCHESTRATION_PRISONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_PATH)
  @Operation(
    summary = "Get all current or future exclude dates for a given prison",
    description = "Get current or future exclude dates for a given prison",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Exclude dates successfully returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonExcludeDateDto::class)),
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
    ],
  )
  fun getFutureExcludeDatesForPrison(
    @Schema(description = "prison code", example = "HEI", required = true)
    @PathVariable
    prisonCode: String,
  ): List<PrisonExcludeDateDto>? {
    return prisonService.getFutureExcludeDatesForPrison(prisonCode)
  }
}
