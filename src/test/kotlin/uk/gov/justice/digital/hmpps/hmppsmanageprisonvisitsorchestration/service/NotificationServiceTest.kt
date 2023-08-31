package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.service.notify.NotificationClient
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {
  private val notificationClient = mock<NotificationClient>()
  private val prisonRegisterClient = mock<PrisonRegisterClient>()

  private lateinit var notificationService: NotificationService

  companion object {
    const val BOOKING_TEMPLATE_ID = "booking-template-id"
    const val UPDATE_TEMPLATE_ID = "update-template-id"
    const val CANCEL_TEMPLATE_ID = "cancel-template-id"
  }

  @BeforeEach
  fun setUp() {
    notificationService = NotificationService(notificationClient, prisonRegisterClient, true, BOOKING_TEMPLATE_ID, UPDATE_TEMPLATE_ID, CANCEL_TEMPLATE_ID)
  }

  @Test
  fun `when a visit is booked then a booking SMS is sent`() {
    // Given
    val prison = IntegrationTestBase.createPrisonDto()
    val visitDate = LocalDate.of(2023, 9, 1)
    val visit = IntegrationTestBase.createVisitDto(startTimestamp = visitDate.atTime(11, 0), endTimestamp = visitDate.atTime(13, 0))

    whenever(
      prisonRegisterClient.getPrison(visit.prisonCode),
    ).thenReturn(prison)

    // When
    notificationService.sendConfirmation(NotificationService.NotificationEvent.VISIT_BOOKING, visit)

    // Then
    verify(notificationClient, times(1)).sendSms(
      BOOKING_TEMPLATE_ID,
      visit.visitContact!!.telephone,
      mapOf(
        "prison" to prison.prisonName,
        "time" to "11:00 am",
        "dayofweek" to "Friday",
        "date" to "01 September 2023",
        "ref number" to visit.reference,
      ),
      null,
    )
  }

  @Test
  fun `when a visit is updated then an update SMS is sent`() {
    // Given
    val prison = IntegrationTestBase.createPrisonDto()
    val visitDate = LocalDate.of(2023, 9, 1)
    val visit = IntegrationTestBase.createVisitDto(startTimestamp = visitDate.atTime(11, 0), endTimestamp = visitDate.atTime(13, 0))

    whenever(
      prisonRegisterClient.getPrison(visit.prisonCode),
    ).thenReturn(prison)

    // When
    notificationService.sendConfirmation(NotificationService.NotificationEvent.VISIT_UPDATE, visit)

    // Then
    verify(notificationClient, times(1)).sendSms(
      UPDATE_TEMPLATE_ID,
      visit.visitContact!!.telephone,
      mapOf(
        "prison" to prison.prisonName,
        "time" to "11:00 am",
        "dayofweek" to "Friday",
        "date" to "01 September 2023",
        "ref number" to visit.reference,
      ),
      null,
    )
  }

  @Test
  fun `when a visit is cancelled then a cancel SMS is sent`() {
    // Given
    val prison = IntegrationTestBase.createPrisonDto()
    val visitDate = LocalDate.of(2023, 9, 1)
    val visit = IntegrationTestBase.createVisitDto(startTimestamp = visitDate.atTime(11, 0), endTimestamp = visitDate.atTime(13, 0))

    whenever(
      prisonRegisterClient.getPrison(visit.prisonCode),
    ).thenReturn(prison)

    // When
    notificationService.sendConfirmation(NotificationService.NotificationEvent.VISIT_CANCEL, visit)

    // Then
    verify(notificationClient, times(1)).sendSms(
      CANCEL_TEMPLATE_ID,
      visit.visitContact!!.telephone,
      mapOf(
        "prison" to prison.prisonName,
        "time" to "11:00 am",
        "date" to "01 September 2023",
        "ref number" to visit.reference,
      ),
      null,
    )
  }

  @Test
  fun `when notifications are disabled then no booking SMS is sent`() {
    // Given
    // notifications are disabled
    notificationService = NotificationService(notificationClient, prisonRegisterClient, false, BOOKING_TEMPLATE_ID, UPDATE_TEMPLATE_ID, CANCEL_TEMPLATE_ID)

    val visitDate = LocalDate.of(2023, 9, 1)
    val visit = IntegrationTestBase.createVisitDto(startTimestamp = visitDate.atTime(11, 0), endTimestamp = visitDate.atTime(13, 0))

    // When
    notificationService.sendConfirmation(NotificationService.NotificationEvent.VISIT_BOOKING, visit)

    // Then
    verify(notificationClient, times(0)).sendSms(any(), any(), any(), anyOrNull())
  }
}
