package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.visitfromexternalsystemevents

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PRISON_VISITS_WRITES_QUEUE_CONFIG_KEY
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.util.UUID

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Visit from external system events")
class VisitFromExternalSystemEventsTest {
  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

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
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "VisitCreated",
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
