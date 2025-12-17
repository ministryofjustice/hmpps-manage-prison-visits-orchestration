package uk.gov.justice.digital.hmpps.orchestration.integration.domainevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.orchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.notifiers.DELETE_INCENTIVES_EVENT_TYPE
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.notifiers.INSERTED_INCENTIVES_EVENT_TYPE
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.notifiers.UPDATED_INCENTIVES_EVENT_TYPE

@TestPropertySource(
  properties = [
    "feature.event.$DELETE_INCENTIVES_EVENT_TYPE=true",
    "feature.event.$INSERTED_INCENTIVES_EVENT_TYPE=false",
  ],
)
internal class EventFeatureSwitchTest : IntegrationTestBase() {

  @Autowired
  private lateinit var featureSwitch: EventFeatureSwitch

  @Test
  fun `should return true when feature is enabled`() {
    assertThat(featureSwitch.isEnabled(DELETE_INCENTIVES_EVENT_TYPE)).isTrue
  }

  @Test
  fun `should return false when feature is disabled`() {
    assertThat(featureSwitch.isEnabled(INSERTED_INCENTIVES_EVENT_TYPE)).isFalse
  }

  @Test
  fun `should return true when feature switch is not present`() {
    assertThat(featureSwitch.isEnabled(UPDATED_INCENTIVES_EVENT_TYPE)).isTrue
  }
}
