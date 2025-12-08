package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerPrisonerValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerVisitorRequestValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.management.SocialContactsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerHistoryAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterVisitorForBookerPrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.VisitorInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.VisitorRequestsCountByPrisonCodeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerDetailedInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerSearchResultsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.SearchBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.BookerPrisonerRegistrationErrorCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PublicBookerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PublicBookerVisitorRequestsService

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker"

const val PUBLIC_BOOKER_DETAILS = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}"
const val PUBLIC_BOOKER_GET_BOOKER_AUDIT_PATH: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}/audit"

const val PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}/permitted/prisoners"
const val PUBLIC_BOOKER_REGISTER_PRISONER_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH/register"
const val PUBLIC_BOOKER_VALIDATE_PRISONER_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH/{prisonerId}/validate"
const val PUBLIC_BOOKER_GET_SOCIAL_CONTACTS_BY_PRISONER_PATH = "$PUBLIC_BOOKER_DETAILS/prisoners/{prisonerId}/social-contacts"

const val PUBLIC_BOOKER_VISITORS_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH/{prisonerId}/permitted/visitors"
const val PUBLIC_BOOKER_UNLINK_VISITOR_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_GET_PRISONERS_CONTROLLER_PATH/{prisonerId}/permitted/visitors/{visitorId}"

const val PUBLIC_BOOKER_SEARCH = "$PUBLIC_BOOKER_CONTROLLER_PATH/search"
const val PUBLIC_BOOKER_CREATE_AUTH_DETAILS_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/register/auth"

// booker - visitor requests
const val PUBLIC_BOOKER_VISITOR_REQUESTS_PATH: String = "$PUBLIC_BOOKER_VISITORS_CONTROLLER_PATH/request"
const val GET_VISITOR_REQUESTS_BY_BOOKER_REFERENCE_AND_PRISONER_ID: String = "$PUBLIC_BOOKER_DETAILS/permitted/visitors/requests"

const val PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE: String = "/prison/{prisonCode}/visitor-requests/count"

@RestController
class PublicBookerController(
  private val publicBookerService: PublicBookerService,
  private val publicBookerVisitorRequestsService: PublicBookerVisitorRequestsService,
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
  @GetMapping(PUBLIC_BOOKER_VISITORS_CONTROLLER_PATH)
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
  @PostMapping(PUBLIC_BOOKER_VISITORS_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Register visitor, for a booker's prisoner",
    description = "Register visitor, for a booker's prisoner",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Registration successful",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to register a visitor, for a booker's prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun registerVisitor(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
    @RequestBody
    registerVisitorForBookerPrisonerDto: RegisterVisitorForBookerPrisonerDto,
  ): PermittedVisitorsForPermittedPrisonerBookerDto = publicBookerService.registerVisitorForBookerPrisoner(bookerReference, prisonerId, registerVisitorForBookerPrisonerDto)

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
  ): List<BookerSearchResultsDto> = publicBookerService.searchForBooker(searchBookerDto)

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

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @DeleteMapping(PUBLIC_BOOKER_UNLINK_VISITOR_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "unlink booker prisoner visitor",
    description = "unlink booker prisoner visitor",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully unlinked booker prisoner visitor",
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
        description = "visitor not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun unlinkBookerPrisonerVisitor(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
    @PathVariable(value = "visitorId", required = true)
    @NotNull
    visitorId: String,
  ) = publicBookerService.unlinkBookerPrisonerVisitor(bookerReference, prisonerId, visitorId)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(PUBLIC_BOOKER_GET_SOCIAL_CONTACTS_BY_PRISONER_PATH)
  @Operation(
    summary = "Get social contacts (not already registered to the booker) for a prisoner associated with that booker.",
    description = "Return social contacts that have not already been registered against the booker given a prison number and booker reference.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Return social contacts (not already registered) for a prisoner associated with that booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get social contacts (not already registered) for a prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booker not authorised / not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get social contacts",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSocialContactsForPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
  ): List<SocialContactsDto> = publicBookerService.getSocialContacts(bookerReference, prisonerId)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @PostMapping(PUBLIC_BOOKER_VISITOR_REQUESTS_PATH)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Submit a request to add a visitor given a prisoner and booker reference.",
    description = "Submit a visitor request to add a visitor given a prisoner and booker reference.",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = AddVisitorToBookerPrisonerRequestDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visitor request submitted successfully",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to submit a visitor request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booker / Prisoner not found for visitor request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "422",
        description = "Visitor request validation failed",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = BookerVisitorRequestValidationErrorResponse::class))],
      ),
    ],
  )
  fun createAddVisitorRequest(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
    @RequestBody
    addVisitorToBookerPrisonerRequestDto: AddVisitorToBookerPrisonerRequestDto,
  ): ResponseEntity<String> {
    publicBookerVisitorRequestsService.createAddVisitorRequest(bookerReference, prisonerId, addVisitorToBookerPrisonerRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED.value()).build()
  }

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISITOR_REQUESTS_BY_BOOKER_REFERENCE_AND_PRISONER_ID)
  @Operation(
    summary = "Get all active visitor requests for a booker.",
    description = "Returns all active visitor requests for a booker, empty if none found.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns all active visitor requests for a booker, empty if none found.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get active visitor requests for a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get active visitor requests",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getActiveVisitorRequestsForBooker(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
  ): List<BookerPrisonerVisitorRequestDto> = publicBookerVisitorRequestsService.getActiveVisitorRequestsForBooker(bookerReference)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE)
  @Operation(
    summary = "Get a count of all visitor requests for a prison via prison code",
    description = "Get a count of all visitor requests for a prison via prison code",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Count successfully returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get count of visitor requests for prison.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get count of visitor requests for prison",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitorRequestsCountByPrisonCode(
    @PathVariable(value = "prisonCode", required = true)
    @NotBlank
    prisonCode: String,
  ): VisitorRequestsCountByPrisonCodeDto = publicBookerVisitorRequestsService.getCountVisitorRequestsForPrison(prisonCode)
}
