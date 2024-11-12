package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerPrisonerValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PublicBookerService

const val PUBLIC_BOOKER_VALIDATE_PRISONER_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}/prisoner/{prisonerId}/validate"

@RestController
class PublicBookerValidationController(
  private val publicBookerService: PublicBookerService,
) {

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(PUBLIC_BOOKER_VALIDATE_PRISONER_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Validates prisoner associated with a booker",
    description = "Validates prisoner associated with a booker",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Validation passed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to validate prisoner associated with a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "422",
        description = "Prisoner validation failed",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = BookerPrisonerValidationException::class))],
      ),
    ],
  )
  fun validatePrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @Parameter(
      description = "Prisoner Id for whom visitors need to be returned.",
      example = "A12345DC",
    )
    @NotBlank
    prisonerId: String,
  ) {
    return publicBookerService.validatePrisoner(bookerReference, prisonerId)
  }
}
