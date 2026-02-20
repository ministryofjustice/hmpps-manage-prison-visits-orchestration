package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import uk.gov.justice.hmpps.kotlin.auth.service.GlobalPrincipalOAuth2AuthorizedClientService
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
  private val prisonerSearchBaseUrl: String,

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

  @param:Value("\${incentives.api.url}")
  private val incentivesApiUrl: String,

  @param:Value("\${api.timeout:10s}")
  private val apiTimeout: Duration,

  @param:Value("\${api.health.timeout:2s}")
  private val healthTimeout: Duration,

  @param:Value("\${gov-uk.api.url}")
  private val govUKApiUrl: String,
) {
  private val clientRegistrationId = "hmpps-api"

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      GlobalPrincipalOAuth2AuthorizedClientService(clientRegistrationRepository),
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun getWebClient(
    url: String,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    url = url,
    registrationId = clientRegistrationId,
    timeout = apiTimeout,
  )

  @Bean
  fun visitSchedulerWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(visitSchedulerBaseUrl, authorizedClientManager, builder)

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(prisonApiBaseUrl, authorizedClientManager, builder)

  @Bean
  fun alertsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(alertsApiBaseUrl, authorizedClientManager, builder)

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(prisonerSearchBaseUrl, authorizedClientManager, builder)

  @Bean
  fun prisonRegisterWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(prisonRegisterBaseUrl, authorizedClientManager, builder)

  @Bean
  fun prisonerContactRegistryWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(prisonerContactRegistryBaseUrl, authorizedClientManager, builder)

  @Bean
  fun manageUsersApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(manageUsersApiBaseUrl, authorizedClientManager, builder)

  @Bean
  fun whereAboutsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(whereAboutsApiUrl, authorizedClientManager, builder)

  @Bean
  fun prisonVisitBookerRegistryWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(prisonVisitBookerRegistryApiUrl, authorizedClientManager, builder)

  @Bean
  fun visitAllocationApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(visitAllocationApiUrl, authorizedClientManager, builder)

  @Bean
  fun incentivesApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(incentivesApiUrl, authorizedClientManager, builder)

  @Bean
  fun govUKWebClient(): WebClient = WebClient.builder().baseUrl(govUKApiUrl).build()

  @Bean
  fun visitSchedulerHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(visitSchedulerBaseUrl, healthTimeout)

  @Bean
  fun prisonApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonApiBaseUrl, healthTimeout)

  @Bean
  fun alertsApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(alertsApiBaseUrl, healthTimeout)

  @Bean
  fun prisonerSearchHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonerSearchBaseUrl, healthTimeout)

  @Bean
  fun prisonRegisterHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonRegisterBaseUrl, healthTimeout)

  @Bean
  fun prisonerContactRegistryHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonerContactRegistryBaseUrl, healthTimeout)

  @Bean
  fun manageUsersApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(manageUsersApiBaseUrl, healthTimeout)

  @Bean
  fun whereAboutsHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(whereAboutsApiUrl, healthTimeout)

  @Bean
  fun prisonVisitBookerRegistryHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(prisonVisitBookerRegistryApiUrl, healthTimeout)

  @Bean
  fun visitAllocationApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(visitAllocationApiUrl, healthTimeout)

  @Bean
  fun incentivesHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(incentivesApiUrl, healthTimeout)
}
