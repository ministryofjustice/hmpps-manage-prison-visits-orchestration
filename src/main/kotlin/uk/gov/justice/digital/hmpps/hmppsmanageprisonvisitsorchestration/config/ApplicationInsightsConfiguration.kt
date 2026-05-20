package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Application insights now controlled by the spring-boot-starter dependency.  However when the key is not specified
 * we don't get a telemetry bean and application won't start.  Therefore need this backup configuration.
 */
@Configuration
class ApplicationInsightsConfiguration {

  @ConditionalOnMissingBean(TelemetryClient::class)
  @Bean
  fun telemetryClient(): TelemetryClient = TelemetryClient()
}

@Suppress("unused")
fun TelemetryClient.trackEvent(name: String, properties: Map<String, String>) = this.trackEvent(name, properties, null)
