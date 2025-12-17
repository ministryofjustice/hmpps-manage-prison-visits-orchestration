package uk.gov.justice.digital.hmpps.orchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.orchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.prisons.ExcludeDateDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.prisons.IsExcludeDateDto
import uk.gov.justice.digital.hmpps.orchestration.service.VisitSchedulerSessionsService
import java.time.LocalDate

const val ORCHESTRATION_SESSIONS_CONFIG_CONTROLLER_PATH: String = "/config/sessions"
const val ORCHESTRATION_SESSIONS_EXCLUDE_DATE_CONTROLLER_PATH: String = "$ORCHESTRATION_SESSIONS_CONFIG_CONTROLLER_PATH/session/{sessionTemplateReference}/exclude-date"
const val ORCHESTRATION_SESSIONS_EXCLUDE_DATE_ADD_CONTROLLER_PATH: String = "$ORCHESTRATION_SESSIONS_EXCLUDE_DATE_CONTROLLER_PATH/add"
const val ORCHESTRATION_SESSIONS_EXCLUDE_DATE_REMOVE_CONTROLLER_PATH: String = "$ORCHESTRATION_SESSIONS_EXCLUDE_DATE_CONTROLLER_PATH/remove"
const val ORCHESTRATION_SESSIONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_PATH: String = "$ORCHESTRATION_SESSIONS_EXCLUDE_DATE_CONTROLLER_PATH/future"
const val ORCHESTRATION_SESSIONS_EXCLUDE_DATE_GET_PAST_CONTROLLER_PATH: String = "$ORCHESTRATION_SESSIONS_EXCLUDE_DATE_CONTROLLER_PATH/past"
const val ORCHESTRATION_SESSIONS_IS_DATE_EXCLUDED_CONTROLLER_PATH: String = "$ORCHESTRATION_SESSIONS_EXCLUDE_DATE_CONTROLLER_PATH/{excludeDate}/isExcluded"

@RestController
class OrchestrationSessionsExcludeDateController(
  private val sessionsService: VisitSchedulerSessionsService,
) {
  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @GetMapping(ORCHESTRATION_SESSIONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_PATH)
  @Operation(
    summary = "Get all current or future exclude dates for a given session template",
    description = "Get current or future exclude dates for a given session template",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Exclude dates successfully returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ExcludeDateDto::class)),
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
        description = "Session template not found on visit-scheduler",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getFutureExcludeDatesForSessionTemplate(
    @Schema(description = "session template reference", example = "aaa-bbb-ccc", required = true)
    @PathVariable
    sessionTemplateReference: String,
  ): List<ExcludeDateDto>? = sessionsService.getFutureExcludeDatesForSessionTemplate(sessionTemplateReference)

  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @GetMapping(ORCHESTRATION_SESSIONS_EXCLUDE_DATE_GET_PAST_CONTROLLER_PATH)
  @Operation(
    summary = "Get all past exclude dates for a given session template",
    description = "Get all past exclude dates for a given session template",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Exclude dates successfully returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ExcludeDateDto::class)),
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
        description = "Session template not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPastExcludeDatesForSessionTemplate(
    @Schema(description = "session template reference", example = "aaa-bbb-ccc", required = true)
    @PathVariable
    sessionTemplateReference: String,
  ): List<ExcludeDateDto>? = sessionsService.getPastExcludeDatesForSessionTemplate(sessionTemplateReference)

  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @PutMapping(ORCHESTRATION_SESSIONS_EXCLUDE_DATE_ADD_CONTROLLER_PATH)
  @Operation(
    summary = "Add exclude date for a given session template",
    description = "Add exclude date for a given session template",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Exclude dates successfully added",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to add exclude date",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to add exclude dates",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Session template not found on visit-scheduler",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun addExcludeDateForSessionTemplate(
    @Schema(description = "session template reference", example = "aaa-bbb-ccc", required = true)
    @PathVariable
    sessionTemplateReference: String,
    @RequestBody @Valid
    excludeDate: ExcludeDateDto,
  ): ResponseEntity<HttpStatus> {
    sessionsService.addExcludeDateForSessionTemplate(sessionTemplateReference, excludeDate)
    return ResponseEntity(HttpStatus.OK)
  }

  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @PutMapping(ORCHESTRATION_SESSIONS_EXCLUDE_DATE_REMOVE_CONTROLLER_PATH)
  @Operation(
    summary = "Remove exclude date for a given session template",
    description = "Remove exclude date for a given session template",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Exclude dates successfully removed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to remove exclude date",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to add exclude dates",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Session Template not found on visit-scheduler",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun removeExcludeDateForSessionTemplate(
    @Schema(description = "session template reference", example = "aaa-bbb-ccc", required = true)
    @PathVariable
    sessionTemplateReference: String,
    @RequestBody @Valid
    excludeDate: ExcludeDateDto,
  ): ResponseEntity<HttpStatus> {
    sessionsService.removeExcludeDateForSessionTemplate(sessionTemplateReference, excludeDate)
    return ResponseEntity(HttpStatus.OK)
  }

  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @GetMapping(ORCHESTRATION_SESSIONS_IS_DATE_EXCLUDED_CONTROLLER_PATH)
  @Operation(
    summary = "Endpoint to check if the date passed has been excluded for visits for the session template",
    description = "Returns true if the date passed has been excluded for visits for the session template, false otherwise.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response if the date is excluded for the session template",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = IsExcludeDateDto::class),
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
        description = "Incorrect permissions to check if date excluded",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Session template not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun isDateExcludedForSessionTemplateVisits(
    @Schema(description = "session template reference", example = "aaa-bbb-ccc", required = true)
    @PathVariable
    sessionTemplateReference: String,
    @Schema(description = "date to be checked if excluded for session template", example = "2024-12-26", required = true)
    @PathVariable
    excludeDate: LocalDate,
  ): IsExcludeDateDto = sessionsService.isDateExcludedForSessionTemplateVisits(sessionTemplateReference, excludeDate)
}
