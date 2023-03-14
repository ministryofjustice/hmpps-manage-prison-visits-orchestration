package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class EventFeatureSwitch(private val environment: Environment) {
  fun isEnabled(eventType: String): Boolean {
    return environment.getProperty("feature.event.$eventType", Boolean::class.java, true)
  }
}
