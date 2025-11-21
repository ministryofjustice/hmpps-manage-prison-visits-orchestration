package uk.gov.justice.digital.hmpps.visits.orchestration.config

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${visit-scheduler.api.url}")
  private val visitSchedulerBaseUrl: String,

  @param:Value("\${prison.api.url}")
  private val prisonApiBaseUrl: String,

  @param:Value("\${alerts.api.url}")
  private val alertsApiBaseUrl: String,

  @param:Value("\${prisoner.search.url}")
  private val prisonSearchBaseUrl: String,

  @param:Value("\${prison-register.api.url}")
  private val prisonRegisterBaseUrl: String,

  @param:Value("\${prisoner-contact.registry.url}")
  private val prisonerContactRegistryBaseUrl: String,

  @param:Value("\${manage-users.api.url}")
  private val manageUsersApiBaseUrl: String,

  @param:Value("\${whereabouts.api.url}")
  private val whereAboutsApiUrl: String,

  @param:Value("\${prison-visit-booker-registry.api.url}")
  private val prisonVisitBookerRegistryApiUrl: String,

  @param:Value("\${visit-allocation.api.url}")
  private val visitAllocationApiUrl: String,

  @param:Value("\${api.timeout:10s}")
  private val apiTimeout: Duration,

  @param:Value("\${api.health-timeout:2s}")
  private val healthTimeout: Duration,

  @param:Value("\${gov-uk.api.url}")
  private val govUKApiUrl: String,
) {
  private enum class HmppsAuthClientRegistrationId(val clientRegistrationId: String) {
    VISIT_SCHEDULER("visit-scheduler"),
    PRISON_API("other-hmpps-apis"),
    PRISONER_SEARCH("other-hmpps-apis"),
    PRISON_REGISTER_CLIENT("other-hmpps-apis"),
    PRISON_CONTACT_REGISTRY_CLIENT("other-hmpps-apis"),
    MANAGE_USERS_API_CLIENT("other-hmpps-apis"),
    WHEREABOUTS_API_CLIENT("other-hmpps-apis"),
    PRISON_VISIT_BOOKER_REGISTRY_API_CLIENT("other-hmpps-apis"),
    ALERTS_API("other-hmpps-apis"),
    VISIT_ALLOCATION_API_CLIENT("other-hmpps-apis"),
  }

  @Bean
  fun customConnectionProvider(): ConnectionProvider = ConnectionProvider.builder("custom")
    .maxConnections(500)
    .maxIdleTime(Duration.ofSeconds(30))
    .maxLifeTime(Duration.ofHours(3))
    .pendingAcquireTimeout(Duration.ofSeconds(60))
    .evictInBackground(Duration.ofSeconds(120))
    .build()

  @Bean
  fun customHttpClient(connectionProvider: ConnectionProvider): HttpClient = HttpClient.create(connectionProvider)
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
    .responseTimeout(Duration.ofSeconds(10))

  @Bean
  fun customWebClientBuilder(httpClient: HttpClient): WebClient.Builder = WebClient.builder()
    .clientConnector(ReactorClientHttpConnector(httpClient))

  @Bean
  fun visitSchedulerWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.VISIT_SCHEDULER.clientRegistrationId, url = visitSchedulerBaseUrl, apiTimeout)

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.PRISON_API.clientRegistrationId, url = prisonApiBaseUrl, apiTimeout)

  @Bean
  fun alertsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.ALERTS_API.clientRegistrationId, url = alertsApiBaseUrl, apiTimeout)

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.PRISONER_SEARCH.clientRegistrationId, url = prisonSearchBaseUrl, apiTimeout)

  @Bean
  fun prisonRegisterWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.PRISON_REGISTER_CLIENT.clientRegistrationId, url = prisonRegisterBaseUrl, apiTimeout)

  @Bean
  fun prisonerContactRegistryWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.PRISON_CONTACT_REGISTRY_CLIENT.clientRegistrationId, url = prisonerContactRegistryBaseUrl, apiTimeout)

  @Bean
  fun manageUsersApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.MANAGE_USERS_API_CLIENT.clientRegistrationId, url = manageUsersApiBaseUrl, apiTimeout)

  @Bean
  fun whereAboutsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.WHEREABOUTS_API_CLIENT.clientRegistrationId, url = whereAboutsApiUrl, apiTimeout)

  @Bean
  fun prisonVisitBookerRegistryWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.PRISON_VISIT_BOOKER_REGISTRY_API_CLIENT.clientRegistrationId, url = prisonVisitBookerRegistryApiUrl, apiTimeout)

  @Bean
  fun visitAllocationApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = HmppsAuthClientRegistrationId.VISIT_ALLOCATION_API_CLIENT.clientRegistrationId, url = visitAllocationApiUrl, apiTimeout)

  @Bean
  fun visitSchedulerHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(visitSchedulerBaseUrl, healthTimeout)

  @Bean
  fun prisonApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonApiBaseUrl, healthTimeout)

  @Bean
  fun prisonSearchHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonSearchBaseUrl, healthTimeout)

  @Bean
  fun prisonRegisterHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonRegisterBaseUrl, healthTimeout)

  @Bean
  fun prisonerContactRegistryHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonerContactRegistryBaseUrl, healthTimeout)

  @Bean
  fun whereAboutsHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(whereAboutsApiUrl, healthTimeout)

  @Bean
  fun prisonVisitBookerRegistryHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonVisitBookerRegistryApiUrl, healthTimeout)

  fun visitAllocationApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonVisitBookerRegistryApiUrl, healthTimeout)

  @Bean
  fun govUKWebClient(): WebClient = WebClient.builder().baseUrl(govUKApiUrl).build()
}
