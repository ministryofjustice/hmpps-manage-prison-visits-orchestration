package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${visit-scheduler.api.url}")
  private val visitSchedulerBaseUrl: String,

  @Value("\${prison.api.url}")
  private val prisonApiBaseUrl: String,

  @Value("\${alerts.api.url}")
  private val alertsApiBaseUrl: String,

  @Value("\${prisoner.search.url}")
  private val prisonSearchBaseUrl: String,

  @Value("\${prison-register.api.url}")
  private val prisonRegisterBaseUrl: String,

  @Value("\${prisoner-contact.registry.url}")
  private val prisonerContactRegistryBaseUrl: String,

  @Value("\${manage-users.api.url}")
  private val manageUsersApiBaseUrl: String,

  @Value("\${whereabouts.api.url}")
  private val whereAboutsApiUrl: String,

  @Value("\${prison-visit-booker-registry.api.url}")
  private val prisonVisitBookerRegistryApiUrl: String,
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
  }

  @Bean
  fun visitSchedulerWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.VISIT_SCHEDULER.clientRegistrationId)
    return getWebClient(visitSchedulerBaseUrl, oauth2Client)
  }

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.PRISON_API.clientRegistrationId)
    return getWebClient(prisonApiBaseUrl, oauth2Client)
  }

  @Bean
  fun alertsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.ALERTS_API.clientRegistrationId)
    return getWebClient(alertsApiBaseUrl, oauth2Client)
  }

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.PRISONER_SEARCH.clientRegistrationId)
    return getWebClient(prisonSearchBaseUrl, oauth2Client)
  }

  @Bean
  fun prisonRegisterWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.PRISON_REGISTER_CLIENT.clientRegistrationId)
    return getWebClient(prisonRegisterBaseUrl, oauth2Client)
  }

  @Bean
  fun prisonerContactRegistryWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.PRISON_CONTACT_REGISTRY_CLIENT.clientRegistrationId)
    return getWebClient(prisonerContactRegistryBaseUrl, oauth2Client)
  }

  @Bean
  fun manageUsersApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.MANAGE_USERS_API_CLIENT.clientRegistrationId)
    return getWebClient(manageUsersApiBaseUrl, oauth2Client)
  }

  @Bean
  fun whereAboutsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.WHEREABOUTS_API_CLIENT.clientRegistrationId)
    return getWebClient(whereAboutsApiUrl, oauth2Client)
  }

  @Bean
  fun prisonVisitBookerRegistryWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.PRISON_VISIT_BOOKER_REGISTRY_API_CLIENT.clientRegistrationId)
    return getWebClient(prisonVisitBookerRegistryApiUrl, oauth2Client)
  }

  private fun getOauth2Client(authorizedClientManager: OAuth2AuthorizedClientManager, clientRegistrationId: String): ServletOAuth2AuthorizedClientExchangeFilterFunction {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(clientRegistrationId)
    return oauth2Client
  }

  private fun getExchangeStrategies(): ExchangeStrategies {
    return ExchangeStrategies.builder()
      .codecs { configurer: ClientCodecConfigurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
      .build()
  }

  private fun getWebClient(baseUrl: String, oauth2Client: ServletOAuth2AuthorizedClientExchangeFilterFunction): WebClient {
    return WebClient.builder()
      .baseUrl(baseUrl)
      .apply(oauth2Client.oauth2Configuration())
      .exchangeStrategies(getExchangeStrategies())
      .build()
  }

  @Bean
  fun visitSchedulerHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(visitSchedulerBaseUrl).build()
  }

  @Bean
  fun prisonApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(prisonApiBaseUrl).build()
  }

  @Bean
  fun prisonSearchHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(prisonSearchBaseUrl).build()
  }

  @Bean
  fun prisonRegisterHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(prisonRegisterBaseUrl).build()
  }

  @Bean
  fun prisonerContactRegistryHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(prisonerContactRegistryBaseUrl).build()
  }

  @Bean
  fun whereAboutsHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(whereAboutsApiUrl).build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
