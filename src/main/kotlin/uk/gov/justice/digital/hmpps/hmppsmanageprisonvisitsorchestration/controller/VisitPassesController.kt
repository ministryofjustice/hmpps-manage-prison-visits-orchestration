package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitPassDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitPassesService
import java.time.LocalDate

const val VISIT_PASSES_CONTROLLER_PATH: String = "/visit-passes/prison/{prisonId}"

@RestController
class VisitPassesController(
  private val visitPassesService: VisitPassesService,
) {
  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(VISIT_PASSES_CONTROLLER_PATH)
  @Operation(
    summary = "Get visit passes for a prison on a given date.",
    description = "Get visit passes for a prison on a given date.",
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
    ],
  )
  fun getVisitPassesForPrisonOnDate(
    @Schema(description = "Prison code.", example = "ABC", required = true)
    @PathVariable(required = true)
    @NotBlank
    prisonId: String,
    @Schema(description = "Date for which visits are being sought.", example = "2026-01-01", required = true)
    @RequestParam
    visitDate: LocalDate,
  ): List<VisitPassDto> {
    println("Getting visit passes for prison $prisonId on date $visitDate")
    return visitPassesService.getVisitPasses(prisonId, visitDate)
  }
}
