package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.BookingOrchestrationRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.CancelVisitOrchestrationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitHistoryDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ChangeVisitSlotRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.ReserveVisitSlotDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.SupportTypeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitMinSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter.VisitSearchRequestFilter
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitsByDateService
import java.time.LocalDate
import java.time.LocalDateTime

const val ORCHESTRATION_VISIT_CONTROLLER_PATH: String = "/visits"

@RestController
class OrchestrationVisitsController(
  private val visitSchedulerService: VisitSchedulerService,
  private val visitsByDateService: VisitsByDateService,
) {
  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @GetMapping("$ORCHESTRATION_VISIT_CONTROLLER_PATH/{reference}")
  @Operation(
    summary = "Get a visit",
    description = "Retrieve a BOOKED or CANCELLED visit by visit reference",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit Information Returned",
      ),
      ApiResponse(
        responseCode = "500",
        description = "Incorrect request to Get visits for prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions retrieve a visit",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visit not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitsByReference(@PathVariable reference: String): VisitDto? {
    return visitSchedulerService.getVisitByReference(reference)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @GetMapping("$ORCHESTRATION_VISIT_CONTROLLER_PATH/{reference}/history")
  @Operation(
    summary = "Get visit history",
    description = "Retrieve visit history by visit reference",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit History Information Returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get visit history",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions retrieve visit history",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visit not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitHistoryByReference(@PathVariable reference: String): VisitHistoryDetailsDto? {
    return visitSchedulerService.getVisitHistoryByReference(reference)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @GetMapping(params = ["page", "size"], path = ["$ORCHESTRATION_VISIT_CONTROLLER_PATH/search"])
  @Operation(
    summary = "Get visits",
    description = "Retrieve visits with optional filters, sorted by start timestamp descending",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit Information Returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get visits for prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve visits",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitsByFilterPageable(
    @RequestParam(value = "prisonerId", required = false)
    @Parameter(
      description = "Filter results by prisoner id",
      example = "A12345DC",
    )
    prisonerId: String?,
    @RequestParam(value = "prisonId", required = false)
    @Parameter(
      description = "Filter results by prison id/code",
      example = "MDI",
    )
    prisonCode: String?,
    @RequestParam(value = "startDateTime", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(
      description = "Filter results by visits that start on or after the given timestamp",
      example = "2021-11-03T09:00:00",
    )
    startDateTime: LocalDateTime?,
    @RequestParam(value = "endDateTime", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(
      description = "Filter results by visits that start on or before the given timestamp",
      example = "2021-11-03T09:00:00",
    )
    endDateTime: LocalDateTime?,
    @RequestParam(value = "visitorId", required = false)
    @Parameter(
      description = "Filter results by visitor (contact id)",
      example = "12322",
    )
    visitorId: Long?,
    @RequestParam(value = "visitStatus", required = true)
    @Parameter(
      description = "Filter results by visit status",
      example = "BOOKED",
    )
    visitStatusList: List<String>,
    @RequestParam(value = "page", required = true)
    @Parameter(
      description = "Pagination page number, starting at zero",
      example = "0",
    )
    page: Int,
    @RequestParam(value = "size", required = true)
    @Parameter(
      description = "Pagination size per page",
      example = "50",
    )
    size: Int,
  ): Page<VisitDto>? {
    val visitSearchRequestFilter = VisitSearchRequestFilter(
      prisonCode = prisonCode,
      prisonerId = prisonerId,
      visitorId = visitorId?.toString(),
      startDateTime = startDateTime,
      endDateTime = endDateTime,
      visitStatusList = visitStatusList,
      page = page,
      size = size,
    )
    return visitSchedulerService.visitsSearch(visitSearchRequestFilter)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @PostMapping("$ORCHESTRATION_VISIT_CONTROLLER_PATH/slot/reserve")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Reserve a slot (date/time slot) for a visit (a starting point)",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ReserveVisitSlotDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visit slot reserved",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to reserve a slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to reserve a slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun reserveVisitSlot(@RequestBody reserveVisitSlotDto: ReserveVisitSlotDto): VisitDto? {
    return visitSchedulerService.reserveVisitSlot(reserveVisitSlotDto)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @PutMapping("$ORCHESTRATION_VISIT_CONTROLLER_PATH/{applicationReference}/book")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Book a visit (end of flow)",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to book a visit",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to book a visit",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visit not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun bookAVisit(
    @Schema(description = "applicationReference", example = "dfs-wjs-eqr", required = true)
    @PathVariable
    applicationReference: String,
    @RequestBody @Valid
    bookingRequestDto: BookingOrchestrationRequestDto,
  ): VisitDto? {
    return visitSchedulerService.bookVisit(applicationReference, bookingRequestDto)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @PutMapping("$ORCHESTRATION_VISIT_CONTROLLER_PATH/{reference}/change")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Change a booked visit, (a starting point)",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ReserveVisitSlotDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visit created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to change a booked visit",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to change a booked visit",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun changeBookedVisit(
    @Schema(description = "reference", example = "v9-d7-ed-7u", required = true)
    @PathVariable
    reference: String,
    @RequestBody @Valid
    reserveVisitSlotDto: ReserveVisitSlotDto,
  ): VisitDto? {
    return visitSchedulerService.changeBookedVisit(reference.trim(), reserveVisitSlotDto)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @PutMapping("$ORCHESTRATION_VISIT_CONTROLLER_PATH/{applicationReference}/slot/change")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Change a reserved slot and associated details for a visit (before booking)",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ChangeVisitSlotRequestDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit slot changed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to changed a visit slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to changed a visit slot",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visit slot not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun changeReservedVisitSlot(
    @Schema(description = "applicationReference", example = "dfs-wjs-eqr", required = true)
    @PathVariable
    applicationReference: String,
    @RequestBody @Valid
    changeVisitSlotRequestDto: ChangeVisitSlotRequestDto,
  ): VisitDto? {
    return visitSchedulerService.changeReservedVisitSlot(applicationReference.trim(), changeVisitSlotRequestDto)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @PutMapping("$ORCHESTRATION_VISIT_CONTROLLER_PATH/{reference}/cancel")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Cancel an existing booked visit",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CancelVisitOrchestrationDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit cancelled",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to cancel a visit",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to cancel a visit",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visit not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun cancelVisit(@PathVariable reference: String, @RequestBody cancelVisitDto: CancelVisitOrchestrationDto): VisitDto? {
    return visitSchedulerService.cancelVisit(reference, cancelVisitDto)
  }

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @GetMapping("/visit-support")
  @Operation(
    summary = "Available Support",
    description = "Retrieve all available support types",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Available Support information returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get Available Support",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSupportTypes(): List<SupportTypeDto>? = visitSchedulerService.getVisitSupport()

  @PreAuthorize("hasRole('VISIT_SCHEDULER')")
  @GetMapping("$ORCHESTRATION_VISIT_CONTROLLER_PATH/session-template/{sessionTemplateReference}")
  @Operation(
    summary = "Get visits for a session template reference and date",
    description = "Retrieve visits for session template reference and date",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit details returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to Get visits for session template",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve visits for session template",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitsBySessionTemplate(@PathVariable sessionTemplateReference: String, @RequestParam sessionDate: LocalDate, @RequestParam visitRestriction: List<VisitRestriction>?): List<VisitMinSummaryDto> {
    return visitsByDateService.getVisitsForSessionTemplateAndDate(sessionTemplateReference, sessionDate, visitRestriction)
  }
}
