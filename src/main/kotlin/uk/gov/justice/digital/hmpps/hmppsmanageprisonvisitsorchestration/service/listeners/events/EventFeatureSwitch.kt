package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class EventFeatureSwitch(
  private val environment: Environment,
  @Value("\${hmpps.sqs.enabled:true}") private val allEventsEnabled: Boolean,
) {

  fun isEnabled(eventType: String): Boolean {
    return isAllEventsEnabled() && environment.getProperty("feature.event.$eventType", Boolean::class.java, true)
  }

  fun isAllEventsEnabled(): Boolean {
    return allEventsEnabled
  }
}
