package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.exceptions.base.MockitoException
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CreateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.UpdateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitExternalSystemDetails
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitNoteDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSubStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitNoteType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.VisitFromExternalSystemEvent
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(SpringExtension::class)
@Timeout(value = 30, unit = TimeUnit.SECONDS)
internal class VisitFromExternalSystemEventListenerServiceTest {
  private val objectMapper = jacksonObjectMapper()
  private val visitSchedulerClient = mock<VisitSchedulerClient>()
  private val visitFromExternalSystemEventListenerService = VisitFromExternalSystemEventListenerService(objectMapper, visitSchedulerClient)
  private val visitDto = VisitDto(
    reference = "v9-d7-ed-7u",
    prisonerId = "A1243B",
    prisonCode = "MKI",
    prisonName = "Millsike",
    visitRoom = "A1",
    visitType = VisitType.SOCIAL,
    visitStatus = VisitStatus.BOOKED,
    visitSubStatus = VisitSubStatus.AUTO_APPROVED,
    outcomeStatus = null,
    visitRestriction = VisitRestriction.OPEN,
    startTimestamp = LocalDateTime.now(),
    endTimestamp = LocalDateTime.now().plusHours(1),
    visitNotes = listOf(VisitNoteDto(type = VisitNoteType.VISITOR_CONCERN, text = "Visitor concern")),
    visitContact = VisitContactDto(
      visitContactId = 1234L,
      name = "John Smith",
      telephone = "01234567890",
      email = "john.smith@example.com",
    ),
    createdTimestamp = LocalDateTime.now(),
    modifiedTimestamp = LocalDateTime.now(),
    visitors = listOf(VisitorDto(nomisPersonId = 1234L, visitContact = true)),
    visitorSupport = VisitorSupportDto(description = "Visual impairement"),
    applicationReference = "abc-123-acd",
    sessionTemplateReference = "abc-123-acd",
    firstBookedDateTime = LocalDateTime.now(),
    visitExternalSystemDetails = VisitExternalSystemDetails(
      clientName = "MLK",
      clientVisitReference = "abc-123-ace",
    ),
  )

  @BeforeEach
  internal fun setUp() {
    Mockito.reset(visitSchedulerClient)
  }

  @Nested
  @DisplayName("Create visit from external system")
  inner class CreateVisit {
    private val messageId = UUID.randomUUID().toString()
    private val visitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = messageId,
      eventType = "VisitCreated",
      messageAttributes = mapOf(
        "prisonerId" to "A1243B",
        "prisonId" to "MKI",
        "clientName" to "client-name",
        "clientVisitReference" to "client-visit-reference",
        "visitRoom" to "A1",
        "visitType" to VisitType.SOCIAL,
        "visitRestriction" to VisitRestriction.OPEN,
        "startTimestamp" to LocalDateTime.now().toString(),
        "endTimestamp" to LocalDateTime.now().plusHours(1).toString(),
        "visitNotes" to listOf(mapOf("type" to VisitNoteType.VISITOR_CONCERN, "text" to "Visitor concern")),
        "visitContact" to mapOf("name" to "John Smith", "telephone" to "01234567890", "email" to "john.smith@example.com"),
        "createDateTime" to LocalDateTime.now().toString(),
        "visitors" to listOf(mapOf("nomisPersonId" to 1234, "visitContact" to true)),
        "visitorSupport" to mapOf("description" to "Visual impairment"),
      ),
    )

    private val invalidVisitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = UUID.randomUUID().toString(),
      eventType = "VisitCreated",
      messageAttributes = mapOf(
        "invalidField" to "OPEN",
      ),
    )

    @Test
    fun `will process a visit create event`() {
      whenever(visitSchedulerClient.createVisitFromExternalSystem(any<CreateVisitFromExternalSystemDto>())).thenReturn(visitDto)

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)

      assertDoesNotThrow {
        visitFromExternalSystemEventListenerService.onEventReceived(message).get()
      }
      verify(visitSchedulerClient, times(1)).createVisitFromExternalSystem(any<CreateVisitFromExternalSystemDto>())
    }

    @Test
    fun `will throw an exception if visit scheduler client returns an error`() {
      val exceptionMessage = "Failed to create visit from external system"
      whenever(visitSchedulerClient.createVisitFromExternalSystem(any<CreateVisitFromExternalSystemDto>())).thenThrow(MockitoException(exceptionMessage))

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)

      val exception = assertThrows<Exception> {
        visitFromExternalSystemEventListenerService.onEventReceived(message).get()
      }
      assertThat(exception.message).contains(exceptionMessage)
      verify(visitSchedulerClient, times(1)).createVisitFromExternalSystem(any<CreateVisitFromExternalSystemDto>())
    }

    @Test
    fun `will throw an exception if message attributes are invalid`() {
      val message = objectMapper.writeValueAsString(invalidVisitFromExternalSystemEvent)

      assertThrows<Exception> {
        visitFromExternalSystemEventListenerService.onEventReceived(message).get()
      }
      verify(visitSchedulerClient, times(0)).createVisitFromExternalSystem(any<CreateVisitFromExternalSystemDto>())
    }
  }

  @Nested
  @DisplayName("Update visit from external system")
  inner class UpdateVisit {
    private val visitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = UUID.randomUUID().toString(),
      eventType = "VisitUpdated",
      messageAttributes = mapOf(
        "visitReference" to "v9-d7-ed-7u",
        "visitRoom" to "A1",
        "visitType" to "SOCIAL",
        "visitRestriction" to "OPEN",
        "startTimestamp" to LocalDateTime.now().toString(),
        "endTimestamp" to LocalDateTime.now().plusHours(1).toString(),
        "visitNotes" to listOf(mapOf("type" to VisitNoteType.VISITOR_CONCERN, "text" to "Visitor concern")),
        "visitContact" to mapOf("name" to "John Smith", "telephone" to "01234567890", "email" to "john.smith@example.com"),
        "visitors" to listOf(mapOf("nomisPersonId" to 1234, "visitContact" to true)),
        "visitorSupport" to mapOf("description" to "Visual impairment"),
      ),
    )

    private val invalidVisitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = UUID.randomUUID().toString(),
      eventType = "VisitUpdated",
      messageAttributes = mapOf(
        "invalidField" to "OPEN",
      ),
    )

    @Test
    fun `will process a visit update event`() {
      whenever(visitSchedulerClient.updateVisitFromExternalSystem(any<UpdateVisitFromExternalSystemDto>())).thenReturn(visitDto)

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)

      assertDoesNotThrow {
        visitFromExternalSystemEventListenerService.onEventReceived(message).get()
      }
      verify(visitSchedulerClient, times(1)).updateVisitFromExternalSystem(any<UpdateVisitFromExternalSystemDto>())
    }

    @Test
    fun `will throw an exception if visit scheduler client returns an error on update event received`() {
      val exceptionMessage = "Could not update visit from external system"
      whenever(visitSchedulerClient.updateVisitFromExternalSystem(any<UpdateVisitFromExternalSystemDto>())).thenThrow(MockitoException(exceptionMessage))

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)

      val exception = assertThrows<Exception> {
        visitFromExternalSystemEventListenerService.onEventReceived(message).get()
      }
      assertThat(exception.message).contains(exceptionMessage)
      verify(visitSchedulerClient, times(1)).updateVisitFromExternalSystem(any<UpdateVisitFromExternalSystemDto>())
    }

    @Test
    fun `will throw an exception if message attributes are invalid`() {
      val message = objectMapper.writeValueAsString(invalidVisitFromExternalSystemEvent)

      assertThrows<Exception> {
        visitFromExternalSystemEventListenerService.onEventReceived(message).get()
      }
      verify(visitSchedulerClient, times(0)).updateVisitFromExternalSystem(any<UpdateVisitFromExternalSystemDto>())
    }
  }

  @Nested
  @DisplayName("Cancel visit from external system")
  inner class CancelVisit {
    private val visitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = UUID.randomUUID().toString(),
      eventType = "VisitCancelled",
      messageAttributes = mapOf(
        "visitReference" to "v9-d7-ed-7u",
        "cancelOutcome" to mapOf("outcomeStatus" to "CANCELLATION", "text" to "Whatever"),
        "userType" to "PRISONER",
        "actionedBy" to "AF34567G",
      ),
    )

    private val invalidVisitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = UUID.randomUUID().toString(),
      eventType = "VisitCancelled",
      messageAttributes = mapOf(
        "invalidField" to "OPEN",
      ),
    )

    @Test
    fun `will process a visit cancel event`() {
      whenever(visitSchedulerClient.cancelVisit(any(), any<CancelVisitDto>())).thenReturn(visitDto)

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)

      assertDoesNotThrow {
        visitFromExternalSystemEventListenerService.onEventReceived(message).get()
      }
      verify(visitSchedulerClient, times(1)).cancelVisit(any(), any<CancelVisitDto>())
    }

    @Test
    fun `will throw an exception if visit scheduler client returns an error on cancel event received`() {
      val exceptionMessage = "Failed to cancel visit from external system"
      whenever(visitSchedulerClient.cancelVisit(any(), any<CancelVisitDto>())).thenThrow(MockitoException(exceptionMessage))

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)

      val exception = assertThrows<Exception> {
        visitFromExternalSystemEventListenerService.onEventReceived(message).get()
      }
      assertThat(exception.message).contains(exceptionMessage)
      verify(visitSchedulerClient, times(1)).cancelVisit(any(), any<CancelVisitDto>())
    }

    @Test
    fun `will throw an exception if message attributes are invalid`() {
      val message = objectMapper.writeValueAsString(invalidVisitFromExternalSystemEvent)

      assertThrows<Exception> {
        visitFromExternalSystemEventListenerService.onEventReceived(message).get()
      }
      verify(visitSchedulerClient, times(0)).cancelVisit(any(), any<CancelVisitDto>())
    }
  }

  @Test
  fun `will throw an an exception when invalid visit write event passed in`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "InvalidEventType",
      "description" : null,
      "messageAttributes" : {
        "prisonerId" : "A1234AB",
        "prisonId" : "MDI",
        "clientVisitReference" : "123456",
        "visitRoom" : "A1",
        "visitType" : "SOCIAL",
        "visitStatus" : "BOOKED",
        "visitRestriction" : "OPEN",
        "startTimestamp" : "2020-12-04T10:42:43",
        "endTimestamp" : "2020-12-04T10:42:43",
        "createDateTime" : "2020-12-04T10:42:43",
        "visitors" : [ {
          "nomisPersonId" : 3,
          "visitContact" : true
        } ],
        "actionedBy" : "automated-test-client"
      },
      "who" : "automated-test-client"
    }
    """

    val exception = assertThrows<Exception> {
      visitFromExternalSystemEventListenerService.onEventReceived(message).get()
    }
    assertThat(exception.message).contains("Cannot process event of type InvalidEventType")
  }
}
