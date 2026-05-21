package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.passes.VisitPassRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitPassesService

const val VISIT_PASSES_CONTROLLER_PATH: String = "/prison/{prisonId}/visit-passes"

@RestController
class VisitPassesController(
  private val visitPassesService: VisitPassesService,
) {
  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @PostMapping(VISIT_PASSES_CONTROLLER_PATH)
  @Operation(
    summary = "Get visit passes for a prison on a given date.",
    description = "Get visit passes for a prison on a given date.",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = VisitPassRequestDto::class),
        ),
      ],
    ),

    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Return visit passes given a prison and visit date.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get visit passes for a prison on a given date.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get visit passes for a prison on a given date.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visit passes could not be found for the supplied prison and visit date.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error while retrieving visit passes.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitPassesForPrisonOnDate(
    @Schema(description = "Prison code.", example = "ABC", required = true)
    @PathVariable(required = true)
    @NotBlank
    prisonId: String,
    @Schema(description = "Visit Pass request details.", required = true)
    @RequestBody
    @Valid
    visitPassRequestDto: VisitPassRequestDto,
  ): List<VisitPassDto> = visitPassesService.getVisitPasses(prisonId, visitPassRequestDto)
}
