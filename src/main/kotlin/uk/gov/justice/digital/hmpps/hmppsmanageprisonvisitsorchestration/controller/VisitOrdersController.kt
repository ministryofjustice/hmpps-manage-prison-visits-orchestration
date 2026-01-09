package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerBalanceAdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.PrisonerBalanceDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation.VisitOrderHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitAllocationService
import java.time.LocalDate

const val VISIT_ORDER_PRISONER_CONTROLLER_PATH: String = "/prison/{prisonId}/prisoners/{prisonerId}/visit-orders"
const val VISIT_ORDER_HISTORY_FOR_PRISONER = "$VISIT_ORDER_PRISONER_CONTROLLER_PATH/history"

const val VISIT_ORDER_PRISONER_BALANCE_ENDPOINT = "$VISIT_ORDER_PRISONER_CONTROLLER_PATH/balance"

@RestController
class VisitOrdersController(
  private val visitAllocationService: VisitAllocationService,
) {
  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(VISIT_ORDER_HISTORY_FOR_PRISONER)
  @Operation(
    summary = "Get visit order history for a prisoner since the from date.",
    description = "Get visit order history for a prisoner since the from date.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Return visit order history for a prisoner since the from date.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get visit order history for a prisoner since the from date.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get visit order history",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitOrderHistoryForPrisoner(
    @PathVariable(value = "prisonId", required = true)
    @NotBlank
    prisonId: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    @Length(min = 3, max = 50)
    prisonerId: String,
    @Schema(description = "Visit order history starting from date", example = "2025-01-01", required = true)
    @RequestParam
    fromDate: LocalDate,
    @Schema(description = "Maximum number of results to return, if null, returns all results from date", example = "100", required = false)
    @RequestParam
    maxResults: Int? = null,
  ): VisitOrderHistoryDetailsDto? = visitAllocationService.getVisitOrderHistoryDetails(prisonId = prisonId, prisonerId = prisonerId, fromDate, maxResults)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(VISIT_ORDER_PRISONER_BALANCE_ENDPOINT)
  @Operation(
    summary = "Get visit order balance of the prisoner",
    description = "Get visit order balance of the prisoner",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Return balance of prisoner",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get visit order balance of prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get visit order balance of prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found on visit allocation api, cannot get balance",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitOrderBalanceForPrisoner(
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    @Length(min = 3, max = 50)
    prisonerId: String,
    @PathVariable(value = "prisonId", required = true)
    @NotBlank
    prisonId: String,
  ): PrisonerBalanceDto = visitAllocationService.getPrisonerVisitOrderBalance(prisonerId, prisonId)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @PutMapping(VISIT_ORDER_PRISONER_BALANCE_ENDPOINT)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Manually adjust a prisoner's visit order balance",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PrisonerBalanceAdjustmentDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner's visit order balance successfully adjusted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request to adjust balance, prisoner on remand or invalid data submitted via DTO",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to adjust a prisoner's visit order balance",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found on visit allocation api, cannot adjust balance",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updatePrisonerVisitOrderBalance(
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    @Length(min = 3, max = 50)
    prisonerId: String,
    @PathVariable(value = "prisonId", required = true)
    @NotBlank
    prisonId: String,
    @RequestBody @Valid
    prisonerBalanceAdjustmentDto: PrisonerBalanceAdjustmentDto,
  ) = visitAllocationService.adjustPrisonerVisitOrderBalance(prisonerId, prisonId, prisonerBalanceAdjustmentDto)
}
