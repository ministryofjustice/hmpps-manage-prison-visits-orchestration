package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.PrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonService

const val ORCHESTRATION_PRISONS_CONFIG_CONTROLLER_PATH: String = "/config/prisons"

@RestController
class OrchestrationPrisonsConfigController(
  private val prisonService: PrisonService,
) {
  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping("$ORCHESTRATION_PRISONS_CONFIG_CONTROLLER_PATH/user-type/{type}/supported")
  @Operation(
    summary = "Get supported prisons",
    description = "Get all supported prisons id's",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Supported prisons returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = String::class)),
            examples = [
              ExampleObject(value = "[\"HEI\", \"MDI\"]"),
            ],
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
        description = "Incorrect permissions to view session templates",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSupportedPrisons(
    @Schema(description = "type", example = "STAFF", required = true)
    @PathVariable
    type: UserType,
  ): List<String>? = prisonService.getSupportedPrisons(type)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping("$ORCHESTRATION_PRISONS_CONFIG_CONTROLLER_PATH/user-type/{type}/supported/detailed")
  @Operation(
    summary = "Get supported prisons with detailed prison details by user type",
    description = "Get all supported prisons with detailed prison details by user type",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Supported prisons returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get supported prison details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSupportedPrisonDetails(
    @Schema(description = "type", example = "STAFF", required = true)
    @PathVariable
    type: UserType,
  ): List<PrisonRegisterPrisonDto> = prisonService.getSupportedPrisonsDetails(type)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping("$ORCHESTRATION_PRISONS_CONFIG_CONTROLLER_PATH/prison/{prisonCode}")
  @Operation(
    summary = "Gets prison by given prison id/code",
    description = "Gets prison by given prison id/code",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "prison returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get prison",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrison(
    @Schema(description = "prison id", example = "BHI", required = true)
    @PathVariable
    prisonCode: String,
  ): PrisonDto = prisonService.getPrisonWithName(prisonCode)
}
