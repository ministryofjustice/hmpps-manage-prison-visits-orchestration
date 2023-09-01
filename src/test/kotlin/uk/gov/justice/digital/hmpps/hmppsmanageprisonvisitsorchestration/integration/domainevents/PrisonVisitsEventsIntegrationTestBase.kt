package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.VisitSchedulerMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.DomainEventListenerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PRISON_VISITS_QUEUE_CONFIG_KEY
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.SQSMessage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesDeletedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesInsertedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesUpdatedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerNonAssociationChangedNotifier
import uk.gov.justice.digital.hmpps.prisonertonomisupdate.integration.LocalStackContainer
import uk.gov.justice.digital.hmpps.prisonertonomisupdate.integration.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class PrisonVisitsEventsIntegrationTestBase {

  companion object {
    private val localStackContainer = LocalStackContainer.instance
    val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())
    val visitSchedulerMockServer = VisitSchedulerMockServer(objectMapper)

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      visitSchedulerMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      visitSchedulerMockServer.stop()
    }
  }

  @Autowired
  lateinit var domainEventListenerService: DomainEventListenerService

  @SpyBean
  lateinit var eventFeatureSwitch: EventFeatureSwitch

  @SpyBean
  lateinit var prisonerIncentivesUpdatedNotifierSpy: PrisonerIncentivesUpdatedNotifier

  @SpyBean
  lateinit var prisonerIncentivesInsertedNotifierSpy: PrisonerIncentivesInsertedNotifier

  @SpyBean
  lateinit var prisonerIncentivesDeletedNotifierSpy: PrisonerIncentivesDeletedNotifier

  @SpyBean
  lateinit var visitSchedulerClient: VisitSchedulerClient

  @SpyBean
  lateinit var nonAssociationChangedNotifier: PrisonerNonAssociationChangedNotifier

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  internal val topic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }

  internal val prisonVisitsEventsQueue by lazy { hmppsQueueService.findByQueueId(PRISON_VISITS_QUEUE_CONFIG_KEY) as HmppsQueue }

  internal val sqsPrisonVisitsEventsClient by lazy { prisonVisitsEventsQueue.sqsClient }
  internal val sqsPrisonVisitsEventsDlqClient by lazy { prisonVisitsEventsQueue.sqsDlqClient }
  internal val prisonVisitsEventsQueueUrl by lazy { prisonVisitsEventsQueue.queueUrl }
  internal val prisonVisitsEventsDlqUrl by lazy { prisonVisitsEventsQueue.dlqUrl }

  internal val awsSnsClient by lazy { topic.snsClient }
  internal val topicArn by lazy { topic.arn }

  @BeforeEach
  fun cleanQueue() {
    purgeQueue(sqsPrisonVisitsEventsClient, prisonVisitsEventsQueueUrl)
    purgeQueue(sqsPrisonVisitsEventsDlqClient!!, prisonVisitsEventsDlqUrl!!)
  }

  fun purgeQueue(client: SqsAsyncClient, url: String) {
    client.purgeQueue(PurgeQueueRequest.builder().queueUrl(url).build()).get()
  }

  fun createDomainEvent(eventType: String, additionalInformation: String = "test"): DomainEvent {
    return DomainEvent(eventType = eventType, additionalInformation)
  }

  fun createSQSMessage(domainEvent: DomainEvent): String {
    val sqaMessage = SQSMessage(type = "Notification", messageId = "123", message = objectMapper.writeValueAsString(domainEvent))
    return objectMapper.writeValueAsString(sqaMessage)
  }

  fun createDomainEventPublishRequest(eventType: String, additionalInformation: String = "test"): PublishRequest? {
    return PublishRequest.builder()
      .topicArn(topicArn)
      .message(objectMapper.writeValueAsString(createDomainEvent(eventType, additionalInformation))).build()
  }
}
