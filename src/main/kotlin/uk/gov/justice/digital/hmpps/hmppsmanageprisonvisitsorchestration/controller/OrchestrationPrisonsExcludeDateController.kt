package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.IsExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.prisons.PrisonExcludeDateDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonService
import java.time.LocalDate

const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_CONFIG_CONTROLLER_PATH/prison/{prisonCode}/exclude-date"
const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_ADD_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH/add"
const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_REMOVE_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH/remove"
const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_GET_FUTURE_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH/future"
const val ORCHESTRATION_PRISONS_EXCLUDE_DATE_GET_PAST_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH/past"
const val ORCHESTRATION_PRISONS_IS_DATE_EXCLUDED_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONS_EXCLUDE_DATE_CONTROLLER_PATH/{excludeDate}/isExcluded"

@RestController
class OrchestrationPrisonsExcludeDateController(
  private val prisonService: PrisonService,
) {
  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
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
      ApiResponse(
        responseCode = "404",
        description = "Prison not found on visit-scheduler",
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

  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @GetMapping(ORCHESTRATION_PRISONS_EXCLUDE_DATE_GET_PAST_CONTROLLER_PATH)
  @Operation(
    summary = "Get all past exclude dates for a given prison",
    description = "Get all past exclude dates for a given prison",
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
      ApiResponse(
        responseCode = "404",
        description = "Prison not found on visit-scheduler",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPastExcludeDatesForPrison(
    @Schema(description = "prison code", example = "HEI", required = true)
    @PathVariable
    prisonCode: String,
  ): List<PrisonExcludeDateDto>? {
    return prisonService.getPastExcludeDatesForPrison(prisonCode)
  }

  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @PutMapping(ORCHESTRATION_PRISONS_EXCLUDE_DATE_ADD_CONTROLLER_PATH)
  @Operation(
    summary = "Add exclude date for a given prison",
    description = "Add exclude date for a given prison",
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
        description = "Prison not found on visit-scheduler",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun addExcludeDateForPrison(
    @Schema(description = "prison code", example = "HEI", required = true)
    @PathVariable
    prisonCode: String,
    @RequestBody @Valid
    prisonExcludeDate: PrisonExcludeDateDto,
  ): ResponseEntity<HttpStatus> {
    prisonService.addExcludeDateForPrison(prisonCode, prisonExcludeDate)
    return ResponseEntity(HttpStatus.OK)
  }

  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @PutMapping(ORCHESTRATION_PRISONS_EXCLUDE_DATE_REMOVE_CONTROLLER_PATH)
  @Operation(
    summary = "Remove exclude date for a given prison",
    description = "Remove exclude date for a given prison",
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
        description = "Prison not found on visit-scheduler",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun removeExcludeDateForPrison(
    @Schema(description = "prison code", example = "HEI", required = true)
    @PathVariable
    prisonCode: String,
    @RequestBody @Valid
    prisonExcludeDate: PrisonExcludeDateDto,
  ): ResponseEntity<HttpStatus> {
    prisonService.removeExcludeDateForPrison(prisonCode, prisonExcludeDate)
    return ResponseEntity(HttpStatus.OK)
  }

  @PreAuthorize("hasAnyRole('VSIP_ORCHESTRATION_SERVICE', 'VISIT_SCHEDULER')")
  @GetMapping(ORCHESTRATION_PRISONS_IS_DATE_EXCLUDED_CONTROLLER_PATH)
  @Operation(
    summary = "Endpoint to check if the date passed has been excluded for visits by the prison",
    description = "Returns true if the date passed has been excluded for visits by the prison, false otherwise.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response if the date is excluded for visits at the prison",
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
        description = "Prison not found on visit-scheduler",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun isDateExcludedForPrisonVisits(
    @Schema(description = "prison code", example = "HEI", required = true)
    @PathVariable
    prisonCode: String,
    @Schema(description = "date to be checked if excluded by prison for visits", example = "2024-12-26", required = true)
    @PathVariable
    excludeDate: LocalDate,
  ): IsExcludeDateDto {
    return prisonService.isDateExcludedForPrisonVisits(prisonCode, excludeDate)
  }
}
