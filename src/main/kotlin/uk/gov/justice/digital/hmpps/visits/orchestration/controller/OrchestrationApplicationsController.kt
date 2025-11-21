package uk.gov.justice.digital.hmpps.visits.orchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.visits.orchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.application.ApplicationDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.application.ChangeApplicationDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler.application.CreateApplicationDto
import uk.gov.justice.digital.hmpps.visits.orchestration.service.ApplicationsService

const val ORCHESTRATION_APPLICATIONS_CONTROLLER_PATH: String = "/visits/application"

const val APPLICATION_RESERVE_SLOT: String = "$ORCHESTRATION_APPLICATIONS_CONTROLLER_PATH/slot/reserve"
const val APPLICATION_RESERVED_SLOT_CHANGE: String = "$ORCHESTRATION_APPLICATIONS_CONTROLLER_PATH/{reference}/slot/change"
const val APPLICATION_CHANGE: String = "$ORCHESTRATION_APPLICATIONS_CONTROLLER_PATH/{bookingReference}/change"

@RestController
class OrchestrationApplicationsController(
  private val applicationsService: ApplicationsService,
) {
  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @PostMapping(APPLICATION_RESERVE_SLOT)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an initial application and reserve a slot",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateApplicationDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visit slot reserved",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to reserve a slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to reserve a slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createInitialApplication(
    @RequestBody @Valid
    createApplicationDto: CreateApplicationDto,
  ): ApplicationDto? = applicationsService.createInitialApplication(createApplicationDto = createApplicationDto)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @PutMapping(APPLICATION_RESERVED_SLOT_CHANGE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Change an incomplete application",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ChangeApplicationDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit slot changed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to changed a visit slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to changed a visit slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visit slot not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun changeIncompleteApplication(
    @Schema(description = "reference", example = "dfs-wjs-eqr", required = true)
    @PathVariable
    reference: String,
    @RequestBody @Valid
    changeApplicationDto: ChangeApplicationDto,
  ): ApplicationDto? = applicationsService.changeIncompleteApplication(reference.trim(), changeApplicationDto)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @PutMapping(APPLICATION_CHANGE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an application for an existing visit",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateApplicationDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visit created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to change a visit",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to change a visit",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createApplicationForAnExistingVisit(
    @Schema(description = "bookingReference", example = "v9-d7-ed-7u", required = true)
    @PathVariable
    bookingReference: String,
    @RequestBody @Valid
    createApplicationDto: CreateApplicationDto,
  ): ApplicationDto? = applicationsService.createApplicationForAnExistingVisit(bookingReference.trim(), createApplicationDto)
}
