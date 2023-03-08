package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PRISON_VISITS_QUEUE_CONFIG_KEY
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesDeletedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesInsertedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesUpdatedNotifier
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

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }

  @SpyBean
  lateinit var prisonerIncentivesUpdatedNotifierSpy: PrisonerIncentivesUpdatedNotifier
  @SpyBean
  lateinit var prisonerIncentivesInsertedNotifierSpy: PrisonerIncentivesInsertedNotifier
  @SpyBean
  lateinit var prisonerIncentivesDeletedNotifierSpy: PrisonerIncentivesDeletedNotifier

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

  fun purgeQueue(client : SqsAsyncClient, url : String) {
    client.purgeQueue(PurgeQueueRequest.builder().queueUrl(url).build()).get()
  }

}


