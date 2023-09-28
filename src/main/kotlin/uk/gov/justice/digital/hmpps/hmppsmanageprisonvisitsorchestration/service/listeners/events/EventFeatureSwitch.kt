package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class EventFeatureSwitch(
  private val environment: Environment,
  @Value("\${hmpps.sqs.enabled:true}") private val allEventsEnabled: Boolean,
) {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    LOG.info("Domain sqs events enabled=${isAllEventsEnabled()}")
  }

  fun isEnabled(eventType: String): Boolean {
    return isAllEventsEnabled() && environment.getProperty("feature.event.$eventType", Boolean::class.java, true)
  }

  fun isAllEventsEnabled(): Boolean {
    return allEventsEnabled
  }
}
