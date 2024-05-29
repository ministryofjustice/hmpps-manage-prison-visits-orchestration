package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.VisitSchedulerMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.DomainEventListenerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PRISON_VISITS_QUEUE_CONFIG_KEY
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PersonRestrictionChangedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesDeletedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesInsertedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesUpdatedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerNonAssociationNotifierAmendedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerNonAssociationNotifierClosedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerNonAssociationNotifierCreatedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerNonAssociationNotifierDeletedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerReceivedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerReleasedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerRestrictionChangedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.VisitorRestrictionChangedNotifier
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class PrisonVisitsEventsIntegrationTestBase {

  companion object {
    private val localStackContainer = LocalStackContainer.instance
    val visitSchedulerMockServer = VisitSchedulerMockServer()

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
  lateinit var personRestrictionChangedNotifierSpy: PersonRestrictionChangedNotifier

  @SpyBean
  lateinit var prisonerReceivedNotifierSpy: PrisonerReceivedNotifier

  @SpyBean
  lateinit var prisonerReleasedNotifierSpy: PrisonerReleasedNotifier

  @SpyBean
  lateinit var prisonerRestrictionChangedNotifierSpy: PrisonerRestrictionChangedNotifier

  @SpyBean
  lateinit var visitorRestrictionChangedNotifierSpy: VisitorRestrictionChangedNotifier

  @SpyBean
  lateinit var domainEventListenerServiceSpy: DomainEventListenerService

  @SpyBean
  lateinit var visitSchedulerClient: VisitSchedulerClient

  @SpyBean
  lateinit var visitSchedulerService: VisitSchedulerService

  @SpyBean
  lateinit var prisonerNonAssociationCreatedNotifier: PrisonerNonAssociationNotifierCreatedNotifier

  @SpyBean
  lateinit var prisonerNonAssociationNotifierClosedNotifier: PrisonerNonAssociationNotifierClosedNotifier

  @SpyBean
  lateinit var prisonerNonAssociationNotifierAmendedNotifier: PrisonerNonAssociationNotifierAmendedNotifier

  @SpyBean
  lateinit var prisonerNonAssociationDeleteNotifier: PrisonerNonAssociationNotifierDeletedNotifier

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

  @Autowired
  lateinit var webTestClient: WebTestClient
  lateinit var roleVisitSchedulerHttpHeaders: (HttpHeaders) -> Unit

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @BeforeEach
  fun cleanQueue() {
    purgeQueue(sqsPrisonVisitsEventsClient, prisonVisitsEventsQueueUrl)
    purgeQueue(sqsPrisonVisitsEventsDlqClient!!, prisonVisitsEventsDlqUrl!!)
    roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ROLE_VISIT_SCHEDULER"))
  }

  @AfterEach
  fun afterEach() {
    visitSchedulerMockServer.resetRequests()
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  fun purgeQueue(client: SqsAsyncClient, url: String) {
    client.purgeQueue(PurgeQueueRequest.builder().queueUrl(url).build()).get()
  }

  fun createDomainEvent(eventType: String, additionalInformation: String = "test"): DomainEvent {
    return DomainEvent(eventType = eventType, additionalInformation)
  }

  fun createDomainEventPublishRequest(eventType: String, domainEvent: String): PublishRequest? {
    return PublishRequest.builder()
      .topicArn(topicArn)
      .message(domainEvent).build()
  }

  fun createDomainEventPublishRequest(eventType: String): PublishRequest? {
    return PublishRequest.builder()
      .topicArn(topicArn)
      .message(objectMapper.writeValueAsString(createDomainEvent(eventType, ""))).build()
  }

  fun createDomainEventJson(eventType: String, additionalInformation: String): String {
    return "{\"eventType\":\"$eventType\",\"additionalInformation\":$additionalInformation}"
  }

  fun createNonAssociationAdditionalInformationJson(): String {
    val jsonVales = HashMap<String, String>()
    jsonVales["nsPrisonerNumber1"] = "A8713DY"
    jsonVales["nsPrisonerNumber2"] = "B2022DY"
    return createAdditionalInformationJson(jsonVales)
  }

  fun createPrisonerReceivedAdditionalInformationJson(prisonerReceivedInfo: PrisonerReceivedInfo): String {
    val jsonVales = HashMap<String, String>()

    jsonVales["nomsNumber"] = prisonerReceivedInfo.prisonerNumber
    jsonVales["reason"] = prisonerReceivedInfo.reason.name
    jsonVales["detail"] = prisonerReceivedInfo.detail
    jsonVales["currentLocation"] = prisonerReceivedInfo.currentLocation.name
    jsonVales["currentPrisonStatus"] = prisonerReceivedInfo.currentPrisonStatus.name
    jsonVales["prisonId"] = prisonerReceivedInfo.prisonCode
    jsonVales["nomisMovementReasonCode"] = prisonerReceivedInfo.nomisMovementReasonCode

    return createAdditionalInformationJson(jsonVales)
  }

  fun createAdditionalInformationJson(
    nomsNumber: String? = null,
    personId: String? = null,
    prisonCode: String? = null,
    effectiveDate: String? = null,
    contactPersonId: String? = null,
    reason: String? = null,
  ): String {
    val jsonVales = HashMap<String, String>()
    nomsNumber?.let {
      jsonVales["nomsNumber"] = nomsNumber
    }
    personId?.let {
      jsonVales["personId"] = personId
    }
    effectiveDate?.let {
      jsonVales["effectiveDate"] = effectiveDate
    }
    contactPersonId?.let {
      jsonVales["contactPersonId"] = contactPersonId
    }
    prisonCode?.let {
      jsonVales["prisonId"] = prisonCode
    }
    reason?.let {
      jsonVales["reason"] = reason
    }
    return createAdditionalInformationJson(jsonVales)
  }

  fun createAdditionalInformationJson(jsonValues: Map<String, String>): String {
    val builder = StringBuilder()
    builder.append("{")
    jsonValues.entries.forEachIndexed { index, entry ->
      builder.append("\"${entry.key}\":\"${entry.value}\"")
      if (index < jsonValues.size - 1) {
        builder.append(",")
      }
    }
    builder.append("}")
    return builder.toString()
  }
}
