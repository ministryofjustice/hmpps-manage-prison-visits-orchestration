package uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.domainevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.notifiers.INSERTED_INCENTIVES_EVENT_TYPE

@TestPropertySource(
  properties = [
    "hmpps.sqs.enabled=false",
  ],
)
internal class EventFeatureSwitchForAllEventsTest : IntegrationTestBase() {

  @Autowired
  private lateinit var featureSwitch: EventFeatureSwitch

  @Test
  fun `should return false when feature is disabled for evenet`() {
    assertThat(featureSwitch.isEnabled(INSERTED_INCENTIVES_EVENT_TYPE)).isFalse
  }

  @Test
  fun `should return false when feature is disabled for all evenets`() {
    assertThat(featureSwitch.isAllEventsEnabled()).isFalse
  }
}
