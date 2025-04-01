package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visitfromexternalsystemevents

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.VisitContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitNoteDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitorSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.OutcomeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitNoteType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.VisitFromExternalSystemEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesInsertedNotifier
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Visit from external system events")
class VisitFromExternalSystemEventsTest: IntegrationTestBase() {
  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @MockitoSpyBean
  lateinit var visitSchedulerClient: VisitSchedulerClient

  protected val vweQueueConfig by lazy {
    hmppsQueueService.findByQueueId(PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY) ?: throw MissingQueueException("HmppsQueue $PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY not found")
  }
  internal val vweQueueSqsClient by lazy { vweQueueConfig.sqsClient }
  internal val vweQueueUrl by lazy { vweQueueConfig.queueUrl }
  internal val vweSqsDlqClient by lazy { vweQueueConfig.sqsDlqClient as SqsAsyncClient }
  internal val vweDlqUrl by lazy { vweQueueConfig.dlqUrl as String }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int = vweQueueSqsClient.countAllMessagesOnQueue(vweQueueUrl).get()
  fun getNumberOfMessagesCurrentlyOnDlq(): Int = vweSqsDlqClient.countAllMessagesOnQueue(vweDlqUrl).get()

  @BeforeEach
  fun `clear queues`() {
    vweQueueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(vweQueueUrl).build())
    vweSqsDlqClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(vweDlqUrl).build())
  }

  @Test
  fun `will process a visit write create event`() {
    val messageId = UUID.randomUUID().toString()
    val visitFromExternalSystemEvent = VisitFromExternalSystemEvent(
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
      )
    )

    val createVisitFromExternalSystemDto = visitFromExternalSystemEvent.toCreateVisitFromExternalSystemDto()
    val visitDto = VisitDto(
      reference = "v9-d7-ed-7u",
      prisonerId = "A1243B",
      prisonCode = "MKI",
      prisonName = "Milsike",
      visitRoom = "A1",
      visitType = VisitType.SOCIAL,
      visitStatus = VisitStatus.BOOKED,
      outcomeStatus = null,
      visitRestriction = VisitRestriction.OPEN,
      startTimestamp = LocalDateTime.now(),
      endTimestamp = LocalDateTime.now().plusHours(1),
      visitNotes = listOf(VisitNoteDto(type = VisitNoteType.VISITOR_CONCERN, text = "Visitor concern")),
      visitContact = VisitContactDto(visitContactId = 1234L, name = "John Smith", telephone = "01234567890", email = "john.smith@example.com"),
      createdTimestamp = LocalDateTime.now(),
      modifiedTimestamp = LocalDateTime.now(),
      visitors = listOf(VisitorDto(nomisPersonId = 1234L, visitContact = true)),
      visitorSupport = VisitorSupportDto(description = "Visual impairement"),
    )
    visitSchedulerMockServer.stubPostVisitFromExternalSystem(createVisitFromExternalSystemDto, visitDto)

    val message = ObjectMapper().writeValueAsString(visitFromExternalSystemEvent)

    vweQueueSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    await untilAsserted { verify(visitSchedulerClient, times(1)).createVisitFromExternalSystem(createVisitFromExternalSystemDto)}
  }

  @Test
  fun `will process a visit write update event`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "VisitUpdated",
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

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
  }

  @Test
  fun `will process a visit write cancelled event`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "VisitCancelled",
      "description" : null,
      "messageAttributes" : {
        "visitReference" : "v9-d7-ed-7u",
        "cancelOutcome": {
          "outcomeStatus": "VISIT_ORDER_CANCELLED",
          "text": "visit order cancelled"
        },
        "actionedBy": "test-consumer"
      }
    }
    """

    vweQueueSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(vweQueueUrl).messageBody(message).build(),
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
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

    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 1 }
  }
}
