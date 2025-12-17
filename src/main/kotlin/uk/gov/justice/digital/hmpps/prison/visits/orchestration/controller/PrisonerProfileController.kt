package uk.gov.justice.digital.hmpps.prison.visits.orchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.PrisonerProfileService

const val ORCHESTRATION_PRISONER_CONTROLLER_PATH: String = "/prisoner"
const val ORCHESTRATION_PRISONER_PROFILE_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONER_CONTROLLER_PATH/{prisonId}/{prisonerId}/profile"

@RestController
class PrisonerProfileController(
  private val prisonerProfileService: PrisonerProfileService,
) {
  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(ORCHESTRATION_PRISONER_PROFILE_CONTROLLER_PATH)
  @Operation(
    summary = "Get a prisoner's profile page",
    description = "Get the prisoner's profile page",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner Profile Returned",
      ),
      ApiResponse(
        responseCode = "500",
        description = "Incorrect request to the prisoner profile page",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve a prisoner's profile",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner profile not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonerProfile(@PathVariable prisonId: String, @PathVariable prisonerId: String): PrisonerProfileDto? = prisonerProfileService.getPrisonerProfile(prisonId, prisonerId)
}
