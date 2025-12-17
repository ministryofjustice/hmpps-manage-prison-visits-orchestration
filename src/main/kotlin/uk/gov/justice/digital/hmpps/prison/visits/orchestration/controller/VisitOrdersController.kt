package uk.gov.justice.digital.hmpps.prison.visits.orchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.allocation.VisitOrderHistoryDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.VisitAllocationService
import java.time.LocalDate

const val VISIT_ORDERS_CONTROLLER_PATH: String = "/visit-orders"
const val VISIT_ORDER_HISTORY_FOR_PRISONER = "$VISIT_ORDERS_CONTROLLER_PATH/{prisonerId}/history"

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
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    @Length(min = 3, max = 50)
    prisonerId: String,
    @RequestParam
    fromDate: LocalDate,
  ): List<VisitOrderHistoryDto> = visitAllocationService.getVisitOrderHistory(prisonerId, fromDate)
}
