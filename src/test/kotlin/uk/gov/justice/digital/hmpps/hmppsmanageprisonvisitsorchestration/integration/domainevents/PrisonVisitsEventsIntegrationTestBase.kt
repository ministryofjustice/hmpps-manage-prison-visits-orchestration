package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.domainevents.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.AlertsApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.VisitSchedulerMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.DomainEventListenerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PRISON_VISITS_QUEUE_CONFIG_KEY
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerReceivedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PersonRestrictionUpsertedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerAlertsUpdatedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesDeletedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesInsertedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerIncentivesUpdatedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerNonAssociationNotifierCreatedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerReceivedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.PrisonerReleasedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.VisitorApprovedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.VisitorRestrictionChangedNotifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.VisitorUnapprovedNotifier
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
    val alertsApiMockServer = AlertsApiMockServer()

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      visitSchedulerMockServer.start()
      alertsApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      visitSchedulerMockServer.stop()
      alertsApiMockServer.stop()
    }
  }

  @MockitoSpyBean
  lateinit var eventFeatureSwitch: EventFeatureSwitch

  @MockitoSpyBean
  lateinit var prisonerIncentivesUpdatedNotifierSpy: PrisonerIncentivesUpdatedNotifier

  @MockitoSpyBean
  lateinit var prisonerIncentivesInsertedNotifierSpy: PrisonerIncentivesInsertedNotifier

  @MockitoSpyBean
  lateinit var prisonerIncentivesDeletedNotifierSpy: PrisonerIncentivesDeletedNotifier

  @MockitoSpyBean
  lateinit var personRestrictionUpsertedNotifierSpy: PersonRestrictionUpsertedNotifier

  @MockitoSpyBean
  lateinit var visitorUnapprovedNotifier: VisitorUnapprovedNotifier

  @MockitoSpyBean
  lateinit var visitorApprovedNotifier: VisitorApprovedNotifier

  @MockitoSpyBean
  lateinit var prisonerReceivedNotifierSpy: PrisonerReceivedNotifier

  @MockitoSpyBean
  lateinit var prisonerReleasedNotifierSpy: PrisonerReleasedNotifier

  @MockitoSpyBean
  lateinit var visitorRestrictionChangedNotifierSpy: VisitorRestrictionChangedNotifier

  @MockitoSpyBean
  lateinit var domainEventListenerServiceSpy: DomainEventListenerService

  @MockitoSpyBean
  lateinit var visitSchedulerClient: VisitSchedulerClient

  @MockitoSpyBean
  lateinit var visitSchedulerService: VisitSchedulerService

  @MockitoSpyBean
  lateinit var prisonerNonAssociationCreatedNotifier: PrisonerNonAssociationNotifierCreatedNotifier

  @MockitoSpyBean
  lateinit var prisonerAlertsUpdatedNotifier: PrisonerAlertsUpdatedNotifier

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

  fun createDomainEventJson(eventType: String, description: String, additionalInformation: String): String {
    return "{\"eventType\":\"$eventType\",\"description\":\"$description\",\"additionalInformation\":$additionalInformation}"
  }
  fun createNonAssociationAdditionalInformationJson(): String {
    val jsonValues = HashMap<String, String>()
    jsonValues["nsPrisonerNumber1"] = "A8713DY"
    jsonValues["nsPrisonerNumber2"] = "B2022DY"
    return createAdditionalInformationJson(jsonValues)
  }

  fun createPrisonerReceivedAdditionalInformationJson(prisonerReceivedInfo: PrisonerReceivedInfo): String {
    val jsonValues = HashMap<String, String>()

    jsonValues["nomsNumber"] = prisonerReceivedInfo.prisonerNumber
    jsonValues["prisonId"] = prisonerReceivedInfo.prisonCode
    jsonValues["reason"] = prisonerReceivedInfo.reason.name

    return createAdditionalInformationJson(jsonValues)
  }

  fun createAlertsUpdatedAdditionalInformationJson(
    prisonerNumber: String,
    bookingId: Long,
    alertsAdded: List<String>? = emptyList(),
    alertsRemoved: List<String>? = emptyList(),
  ): String {
    val jsonValues = HashMap<String, Any>()
    jsonValues["nomsNumber"] = prisonerNumber
    jsonValues["bookingId"] = bookingId
    jsonValues["alertsAdded"] = alertsAdded ?: emptyList<String>()
    jsonValues["alertsRemoved"] = alertsRemoved ?: emptyList<String>()
    return createAdditionalInformationJson(jsonValues)
  }

  fun createAdditionalInformationJson(
    nomsNumber: String? = null,
    personId: String? = null,
    prisonCode: String? = null,
    effectiveDate: String? = null,
    contactPersonId: String? = null,
    reason: String? = null,
  ): String {
    val jsonValues = HashMap<String, Any>()
    nomsNumber?.let {
      jsonValues["nomsNumber"] = nomsNumber
    }
    personId?.let {
      jsonValues["personId"] = personId
    }
    effectiveDate?.let {
      jsonValues["effectiveDate"] = effectiveDate
    }
    contactPersonId?.let {
      jsonValues["contactPersonId"] = contactPersonId
    }
    prisonCode?.let {
      jsonValues["prisonId"] = prisonCode
    }
    reason?.let {
      jsonValues["reason"] = reason
    }
    return createAdditionalInformationJson(jsonValues)
  }

  fun createPersonRestrictionAdditionalInformationJson(
    nomsNumber: String? = null,
    visitorId: String? = null,
    effectiveDate: String? = null,
    expiryDate: String? = null,
    restrictionType: String? = null,
  ): String {
    val jsonValues = HashMap<String, Any>()
    nomsNumber?.let {
      jsonValues["nomsNumber"] = nomsNumber
    }
    visitorId?.let {
      jsonValues["personId"] = visitorId
    }
    effectiveDate?.let {
      jsonValues["effectiveDate"] = effectiveDate
    }
    expiryDate?.let {
      jsonValues["expiryDate"] = expiryDate
    }
    restrictionType?.let {
      jsonValues["restrictionType"] = restrictionType
    }

    return createAdditionalInformationJson(jsonValues)
  }

  private fun createAdditionalInformationJson(jsonValues: Map<String, Any>): String {
    val builder = StringBuilder()
    builder.append("{")
    jsonValues.entries.forEachIndexed { index, entry ->
      builder.append(getJsonString(entry))

      if (index < jsonValues.size - 1) {
        builder.append(",")
      }
    }
    builder.append("}")
    return builder.toString()
  }

  private fun getJsonString(entry: Map.Entry<String, Any>): String {
    return when (entry.value) {
      is List<*> -> {
        ("\"${entry.key}\":[${(entry.value as List<*>).joinToString { "\"" + it + "\"" }}]")
      }

      is Number -> {
        ("\"${entry.key}\":${entry.value}")
      }

      else -> {
        ("\"${entry.key}\":\"${entry.value}\"")
      }
    }
  }
}
