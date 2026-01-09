package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visit.external.system

import org.awaitility.Awaitility
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
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
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents.PrisonVisitsEventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.VisitFromExternalSystemEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@DisplayName("Visit from external system events")
class VisitFromExternalSystemEventsTest : PrisonVisitsEventsIntegrationTestBase() {

  companion object {
    @JvmStatic
    @BeforeAll
    fun awaitilityDefaults() {
      Awaitility.setDefaultTimeout(Duration.ofSeconds(30))
    }
  }

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  private val vweQueueConfig by lazy {
    hmppsQueueService.findByQueueId(PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY)
      ?: throw MissingQueueException("HmppsQueue $PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY not found")
  }

  internal val vweQueueSqsClient by lazy { vweQueueConfig.sqsClient }
  internal val vweQueueUrl by lazy { vweQueueConfig.queueUrl }
  internal val vweSqsDlqClient by lazy { vweQueueConfig.sqsDlqClient as SqsAsyncClient }
  internal val vweDlqUrl by lazy { vweQueueConfig.dlqUrl as String }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int = vweQueueSqsClient.countAllMessagesOnQueue(vweQueueUrl).get()
  fun getNumberOfMessagesCurrentlyOnDlq(): Int = vweSqsDlqClient.countAllMessagesOnQueue(vweDlqUrl).get()

  private fun awaitQueueEmpty() {
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
  }

  private fun awaitDlqHasOneMessage() {
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 1 }
  }

  private fun awaitQueuesEmptyAfterPurge() {
    await().atMost(Duration.ofSeconds(30))
      .untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    await().atMost(Duration.ofSeconds(30))
      .untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
  }

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
  fun clearQueues() {
    vweQueueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(vweQueueUrl).build()).get()
    vweSqsDlqClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(vweDlqUrl).build()).get()

    awaitQueuesEmptyAfterPurge()
  }

  @Nested
  @DisplayName("create visit from external system")
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
        "visitorSupport" to mapOf("description" to "Visual impairement"),
      ),
    )

    private val invalidVisitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = UUID.randomUUID().toString(),
      eventType = "VisitCreated",
      messageAttributes = mapOf(
        "invalidField" to "OPEN",
      ),
    )

    private val createVisitFromExternalSystemDto = visitFromExternalSystemEvent.toCreateVisitFromExternalSystemDto()

    @Test
    fun `will process a visit write create event`() {
      visitSchedulerMockServer.stubPostVisitFromExternalSystem(createVisitFromExternalSystemDto, visitDto)

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)
      vweQueueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
      )

      await untilAsserted {
        verify(visitSchedulerClient, times(1)).createVisitFromExternalSystem(any<CreateVisitFromExternalSystemDto>())
      }
      awaitQueueEmpty()
    }

    @Test
    fun `if visit scheduler returns error, expect event to be added to the dlq`() {
      visitSchedulerMockServer.stubPostVisitFromExternalSystem(
        createVisitFromExternalSystemDto,
        visitDto,
        status = HttpStatus.BAD_REQUEST,
      )

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)
      vweQueueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
      )

      awaitDlqHasOneMessage()
      await untilAsserted {
        verify(visitSchedulerClient, times(1)).createVisitFromExternalSystem(any<CreateVisitFromExternalSystemDto>())
      }
    }

    @Test
    fun `will throw an exception if message attributes are invalid`() {
      val message = objectMapper.writeValueAsString(invalidVisitFromExternalSystemEvent)
      vweQueueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
      )

      awaitDlqHasOneMessage()

      await untilAsserted {
        verify(visitSchedulerClient, times(0)).createVisitFromExternalSystem(any<CreateVisitFromExternalSystemDto>())
      }
    }
  }

  @Nested
  @DisplayName("Update visit from external system")
  inner class UpdateVisit {
    private val messageId = UUID.randomUUID().toString()

    private val visitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = messageId,
      eventType = "VisitUpdated",
      messageAttributes = mapOf(
        "visitReference" to "v9-d7-ed-7u",
        "visitRoom" to "A1",
        "visitType" to VisitType.SOCIAL,
        "visitRestriction" to VisitRestriction.OPEN,
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

    private val updateVisitFromExternalSystemDto = visitFromExternalSystemEvent.toUpdateVisitFromExternalSystemDto()

    @Test
    fun `will process a visit write update event`() {
      visitSchedulerMockServer.stubPutVisitFromExternalSystem(updateVisitFromExternalSystemDto, visitDto)

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)
      vweQueueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
      )

      await untilAsserted {
        verify(visitSchedulerClient, times(1)).updateVisitFromExternalSystem(any<UpdateVisitFromExternalSystemDto>())
      }
      awaitQueueEmpty()
    }

    @Test
    fun `if visit scheduler returns error, expect event to be added to the dlq`() {
      visitSchedulerMockServer.stubPutVisitFromExternalSystem(updateVisitFromExternalSystemDto, visitDto, status = HttpStatus.BAD_REQUEST)

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)
      vweQueueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
      )

      awaitDlqHasOneMessage()
      await untilAsserted {
        verify(visitSchedulerClient, times(1)).updateVisitFromExternalSystem(any<UpdateVisitFromExternalSystemDto>())
      }
    }

    @Test
    fun `will throw an exception if message attributes are invalid`() {
      val message = objectMapper.writeValueAsString(invalidVisitFromExternalSystemEvent)
      vweQueueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
      )

      awaitDlqHasOneMessage()
      await untilAsserted {
        verify(visitSchedulerClient, times(0)).updateVisitFromExternalSystem(any<UpdateVisitFromExternalSystemDto>())
      }
    }
  }

  @Nested
  @DisplayName("cancel visit from external system")
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
      val visitRef = visitFromExternalSystemEvent.messageAttributes["visitReference"].toString()
      visitSchedulerMockServer.stubCancelVisit(visitRef, visitDto)

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)
      vweQueueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
      )

      await untilAsserted { verify(visitSchedulerClient, times(1)).cancelVisit(any(), any<CancelVisitDto>()) }
      awaitQueueEmpty()
    }

    @Test
    fun `if visit scheduler returns error, expect event to be added to the dlq`() {
      val visitRef = visitFromExternalSystemEvent.messageAttributes["visitReference"].toString()
      visitSchedulerMockServer.stubCancelVisit(visitRef, null)

      val message = objectMapper.writeValueAsString(visitFromExternalSystemEvent)
      vweQueueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
      )

      awaitDlqHasOneMessage()
    }

    @Test
    fun `will throw an exception if message attributes are invalid`() {
      val message = objectMapper.writeValueAsString(invalidVisitFromExternalSystemEvent)
      vweQueueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
      )

      awaitDlqHasOneMessage()
      await untilAsserted {
        verify(visitSchedulerClient, times(0)).cancelVisit(any(), any<CancelVisitDto>())
      }
    }
  }

  @Test
  fun `will write an invalid visit write event to the dlq`() {
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

    vweQueueSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
    )

    awaitDlqHasOneMessage()
  }
}
