package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.BasicContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonerProfileService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitorProfileService

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker/"

const val BOOKER_LINKED_PRISONERS: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/prisoner/{prisonerIds}"
const val BOOKER_LINKED_PRISONER_VISITORS: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/prisoner/{prisonerId}/visitors/{visitorIds}"

@RestController
class OrchestrationPublicBookerController(
  val prisonerProfileService: PrisonerProfileService,
  val visitorProfileService: VisitorProfileService,
) {
  @PreAuthorize("hasRole('PUBLIC_BOOKER')")
  @GetMapping(BOOKER_LINKED_PRISONERS)
  @Operation(
    summary = "Get prisoners associated with a booker.",
    description = "Get prisoners associated with a booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned prisoners associated with a booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get prisoners associated with a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get prisoners associated with a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getBasicPrisonerDetails(
    @PathVariable(value = "prisonerIds", required = true)
    @Parameter(
      description = "Prisoner ID for whom visitors need to be returned.",
      example = "A12345DC",
    )
    @NotEmpty
    prisonerIds: List<String>,
  ): List<PrisonerBasicInfoDto> {
    return prisonerProfileService.getPrisonerDetails(prisonerIds)
  }

  @PreAuthorize("hasRole('PUBLIC_BOOKER')")
  @GetMapping(BOOKER_LINKED_PRISONER_VISITORS)
  @Operation(
    summary = "Get visitors for a prisoner associated with that booker.",
    description = "Get visitors for a prisoner associated with that booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned visitors for a prisoner associated with that booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get visitors for a prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get visitors for a prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitorsForPrisonerAssociatedWithBooker(
    @PathVariable(value = "prisonerId", required = true)
    @Parameter(
      description = "Prisoner ID for whom visitors need to be returned.",
      example = "A12345DC",
    )
    @NotBlank
    prisonerId: String,
    @PathVariable(value = "visitorIds", required = true)
    @Parameter(
      description = "Prisoner ID for whom visitors need to be returned.",
      example = "A12345DC",
    )
    @NotEmpty
    visitorIds: List<Long>,
  ): List<BasicContactDto> {
    return visitorProfileService.getVisitorsDetails(prisonerId, visitorIds)
  }
}
