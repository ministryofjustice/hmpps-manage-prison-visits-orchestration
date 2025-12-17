package uk.gov.justice.digital.hmpps.orchestration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.orchestration.config.ErrorResponse
import uk.gov.justice.digital.hmpps.orchestration.dto.orchestration.IgnoreVisitNotificationsOrchestrationDto
import uk.gov.justice.digital.hmpps.orchestration.dto.orchestration.OrchestrationVisitNotificationsDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.IgnoreVisitNotificationsDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.visitnotification.NotificationCountDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.visitnotification.NotificationEventType
import uk.gov.justice.digital.hmpps.orchestration.service.VisitSchedulerService

const val VISIT_NOTIFICATION_CONTROLLER_PATH: String = "/visits/notification"
const val VISIT_NOTIFICATION_COUNT_FOR_PRISON_PATH: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/{prisonCode}/count"
const val FUTURE_NOTIFICATION_VISITS: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/{prisonCode}/visits"
const val VISIT_NOTIFICATION_IGNORE: String = "$VISIT_NOTIFICATION_CONTROLLER_PATH/visit/{reference}/ignore"

@RestController
@Validated
@Tag(name = "Visit notification controller $VISIT_NOTIFICATION_CONTROLLER_PATH")
@RequestMapping(name = "Visit notification Resource", produces = [MediaType.APPLICATION_JSON_VALUE])
class VisitNotificationController(
  private val visitSchedulerService: VisitSchedulerService,
) {

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(VISIT_NOTIFICATION_COUNT_FOR_PRISON_PATH)
  @Operation(
    summary = "Get notification count for a prison",
    description = "Retrieve notification count by prison code",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Retrieve notification count for a prison",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getNotificationCountForPrison(
    @Schema(description = "prisonCode", example = "CFI", required = true)
    @PathVariable
    prisonCode: String,
    @Schema(description = "list of notificationEventTypes", required = false)
    @RequestParam(value = "types", required = false)
    notificationEventTypes: List<NotificationEventType>? = null,
  ): NotificationCountDto? = visitSchedulerService.getNotificationCountForPrison(prisonCode, notificationEventTypes)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(FUTURE_NOTIFICATION_VISITS)
  @Operation(
    summary = "get future visits with notifications by prison code",
    description = "Retrieve future visits that have a notification event attribute associated, empty response if no future visits with notifications found.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Retrieved future visits with notifications by prison code",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getFutureNotificationVisits(
    @Schema(description = "prisonCode", example = "CFI", required = true)
    @PathVariable
    prisonCode: String,
    @Schema(description = "list of notificationEventTypes", required = false)
    @RequestParam(value = "types", required = false)
    notificationEventTypes: List<NotificationEventType>?,
  ): List<OrchestrationVisitNotificationsDto> = visitSchedulerService.getFutureVisitsWithNotifications(prisonCode, notificationEventTypes)

  @PreAuthorize("hasAnyRole('VISIT_SCHEDULER', 'VSIP_ORCHESTRATION_SERVICE')")
  @PutMapping(VISIT_NOTIFICATION_IGNORE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Do not change an existing booked visit and ignore all notifications",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = IgnoreVisitNotificationsDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit notifications cleared and reason noted.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to ignore visit notifications.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to ignore visit notifications.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visit not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun ignoreVisitNotifications(
    @Schema(description = "reference", example = "v9-d7-ed-7u", required = true)
    @PathVariable
    reference: String,
    @RequestBody @Valid
    ignoreVisitNotifications: IgnoreVisitNotificationsOrchestrationDto,
  ): VisitDto? = visitSchedulerService.ignoreVisitNotifications(reference.trim(), ignoreVisitNotifications)
}
