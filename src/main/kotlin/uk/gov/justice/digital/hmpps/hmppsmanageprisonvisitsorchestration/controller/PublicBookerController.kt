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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerPrisonerValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerHistoryAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.VisitorInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerDetailedInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.SearchBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.BookerPrisonerRegistrationErrorCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PublicBookerService

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker"
const val PUBLIC_BOOKER_CREATE_AUTH_DETAILS_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/register/auth"
const val PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}/permitted/prisoners"
const val PUBLIC_BOOKER_REGISTER_PRISONER_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH/register"
const val PUBLIC_BOOKER_GET_VISITORS_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH/{prisonerId}/permitted/visitors"
const val PUBLIC_BOOKER_VALIDATE_PRISONER_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH/{prisonerId}/validate"
const val PUBLIC_BOOKER_GET_BOOKER_AUDIT_PATH: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}/audit"
const val PUBLIC_BOOKER_SEARCH = "$PUBLIC_BOOKER_CONTROLLER_PATH/search"
const val PUBLIC_BOOKER_DETAILS = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}"

@RestController
class PublicBookerController(
  private val publicBookerService: PublicBookerService,
) {

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
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
  fun bookerAuthorisation(@RequestBody @Valid authDetail: AuthDetailDto): BookerReference = publicBookerService.bookerAuthorisation(authDetail)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH)
  @Operation(
    summary = "Get permitted prisoners associated with a booker.",
    description = "Get permitted prisoners associated with a booker.",
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
  fun getPermittedPrisonersForBooker(
    @PathVariable(value = "bookerReference", required = true)
    @Parameter(
      description = "Booker's unique reference.",
      example = "A12345DC",
    )
    @NotBlank
    bookerReference: String,
  ): List<BookerPrisonerInfoDto> = publicBookerService.getPermittedPrisonersForBooker(bookerReference)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(PUBLIC_BOOKER_GET_VISITORS_CONTROLLER_PATH)
  @Operation(
    summary = "Get permitted visitors for a prisoner associated with that booker.",
    description = "Get permitted visitors for a prisoner associated with that booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned permitted permitted visitors for a prisoner associated with that booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get permitted visitors for a prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get permitted visitors for a prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPermittedVisitorsForPrisoner(
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
  ): List<VisitorInfoDto> = publicBookerService.getPermittedVisitorsForPermittedPrisonerAndBooker(bookerReference, prisonerId)

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
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = BookerPrisonerValidationErrorResponse::class),
          ),
        ],
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
  ) = publicBookerService.validatePrisoner(bookerReference, prisonerId)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @PostMapping(PUBLIC_BOOKER_REGISTER_PRISONER_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Register prisoner to a booker",
    description = "Register prisoner to a booker",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Registration successful",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to register a prisoner to a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "422",
        description = "Prisoner registration failed",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = BookerPrisonerRegistrationErrorCodes::class),
          ),
        ],
      ),
    ],
  )
  fun registerPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @RequestBody
    registerPrisonerForBookerDto: RegisterPrisonerForBookerDto,
  ) = publicBookerService.registerPrisoner(bookerReference, registerPrisonerForBookerDto)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(PUBLIC_BOOKER_GET_BOOKER_AUDIT_PATH)
  @Operation(
    summary = "Get audit entries for a booker.",
    description = "Get audit entries for a booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Return all audit entries for booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get audit entries for a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get audit entries for a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getBookerAudit(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
  ): List<BookerHistoryAuditDto> = publicBookerService.getBookerAudit(bookerReference)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @PostMapping(PUBLIC_BOOKER_SEARCH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Search for booker(s) via email",
    description = "Search for all booker accounts that are registered to email",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Booker(s) found successfully",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to search for booker(s)",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun searchForBooker(
    @RequestBody
    searchBookerDto: SearchBookerDto,
  ): List<BookerInfoDto> = publicBookerService.searchForBooker(searchBookerDto)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(PUBLIC_BOOKER_DETAILS)
  @Operation(
    summary = "Get detailed information for a booker (including prisoners and visitors) via booker reference.",
    description = "Returns detailed information for a booker (including prisoners and visitors) via booker reference.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Return details",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get booker details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get booker details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getBookerDetails(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
  ): BookerDetailedInfoDto = publicBookerService.getBookerDetails(bookerReference)
}
