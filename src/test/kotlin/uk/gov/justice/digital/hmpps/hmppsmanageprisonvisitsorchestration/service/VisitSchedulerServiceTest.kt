package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitDetailsClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.BookingOrchestrationRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.BookingRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase

@ExtendWith(MockitoExtension::class)
class VisitSchedulerServiceTest {
  private val visitSchedulerClient = mock<VisitSchedulerClient>()
  private val visitDetailsClient = mock<VisitDetailsClient>()
  private val authenticationHelperService = mock<AuthenticationHelperService>()
  private val notificationService = mock<NotificationService>()

  private lateinit var visitSchedulerService: VisitSchedulerService

  @BeforeEach
  fun setUp() {
    visitSchedulerService = VisitSchedulerService(visitSchedulerClient, visitDetailsClient, authenticationHelperService, notificationService)
  }

  @Test
  fun `when visit is successfully booked then a confirmation SMS is sent`() {
    // Given
    val visitDto = IntegrationTestBase.createVisitDto()
    val applicationReference = "dummy-reference"
    val actionedBy = "dummy-user"
    val bookingRequestDto = BookingRequestDto(actionedBy, ApplicationMethodType.NOT_APPLICABLE)

    whenever(
      visitSchedulerClient.bookVisitSlot(applicationReference, bookingRequestDto),
    ).thenReturn(visitDto)

    whenever(
      authenticationHelperService.currentUserName,
    ).thenReturn(actionedBy)

    // When
    visitSchedulerService.bookVisit(applicationReference, BookingOrchestrationRequestDto(bookingRequestDto.applicationMethodType))

    // Then
    verify(notificationService, times(1)).sendConfirmation(NotificationService.NotificationEvent.VISIT_BOOKING, visitDto)
  }

  @Test
  fun `when visit booking is unsuccessful then confirmation SMS is not sent`() {
    // Given
    val applicationReference = "dummy-reference"
    val actionedBy = "dummy-user"
    val bookingRequestDto = BookingRequestDto(actionedBy, ApplicationMethodType.NOT_APPLICABLE)

    whenever(
      visitSchedulerClient.bookVisitSlot(applicationReference, bookingRequestDto),
    ).thenReturn(null)

    whenever(
      authenticationHelperService.currentUserName,
    ).thenReturn(actionedBy)

    // When
    visitSchedulerService.bookVisit(applicationReference, BookingOrchestrationRequestDto(bookingRequestDto.applicationMethodType))

    // Then
    verify(notificationService, times(0)).sendConfirmation(any(), any())
  }
}
