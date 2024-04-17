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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitorProfileService

const val ORCHESTRATION_VISITORS_CONTROLLER_PATH: String = "$ORCHESTRATION_PRISONER_CONTROLLER_PATH/{prisonerId}/visitors"
const val ORCHESTRATION_VISITORS_BASIC_INFO_CONTROLLER_PATH: String = "$ORCHESTRATION_VISITORS_CONTROLLER_PATH/{visitorIds}/basic-details"

@RestController
class VisitorProfileController(
  private val visitorProfileService: VisitorProfileService,
) {
  @PreAuthorize("hasRole('ROLE_ORCHESTRATION_SERVICE__VISIT_BOOKER_REGISTRY')")
  @GetMapping(ORCHESTRATION_VISITORS_BASIC_INFO_CONTROLLER_PATH)
  @Operation(
    summary = "Get visitor(s) basic details",
    description = "Get visitor(s) basic details",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visitor(s) details returned",
      ),
      ApiResponse(
        responseCode = "500",
        description = "Incorrect request to retrieve visitor(s) basic details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve visitor's basic details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visitor details not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitorsBasicDetails(@PathVariable prisonerId: String, @PathVariable visitorIds: List<Long>): List<VisitorBasicInfoDto> {
    return visitorProfileService.getVisitorsDetails(prisonerId, visitorIds)
  }
}
