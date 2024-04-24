package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PublicBookerService

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker"
const val PUBLIC_BOOKER_CREATE_AUTH_DETAILS_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/register/auth"
const val PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}/prisoners"
const val PUBLIC_BOOKER_GET_VISITORS_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH/{prisonerNumber}/visitors"

@RestController
class PublicBookerController(
  private val publicBookerService: PublicBookerService,
) {

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @PutMapping(PUBLIC_BOOKER_CREATE_AUTH_DETAILS_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Authenticate one login details against pre populated bookers",
    description = "Authenticate one login details against pre populated bookers and return BookerReference object to be used for all other api calls for booker information",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "One login details matched with pre populated booker",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booker not authorised / not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun bookerAuthorisation(@RequestBody @Valid authDetail: AuthDetailDto): BookerReference {
    return publicBookerService.bookerAuthorisation(authDetail)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @GetMapping(PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH)
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
  fun getPrisonersForBooker(
    @PathVariable(value = "bookerReference", required = true)
    @Parameter(
      description = "Booker's unique reference.",
      example = "A12345DC",
    )
    @NotBlank
    bookerReference: String,
  ): List<PrisonerBasicInfoDto> {
    return publicBookerService.getBookersPrisoners(bookerReference)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @GetMapping(PUBLIC_BOOKER_GET_VISITORS_CONTROLLER_PATH)
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
  fun getVisitorsForPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerNumber", required = true)
    @Parameter(
      description = "Prisoner Number for whom visitors need to be returned.",
      example = "A12345DC",
    )
    @NotBlank
    prisonerNumber: String,
  ): List<VisitorBasicInfoDto> {
    return publicBookerService.getVisitorsForBookersPrisoner(bookerReference, prisonerNumber)
  }
}
