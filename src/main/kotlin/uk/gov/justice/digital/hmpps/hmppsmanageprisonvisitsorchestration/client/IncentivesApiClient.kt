package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.incentives.IncentiveLevelDto
import java.time.Duration

@Component
class IncentivesApiClient(
  @param:Qualifier("incentivesApiWebClient")
  private val webClient: WebClient,
  @param:Value("\${incentives.api.timeout:10s}")
  private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getAllIncentiveLevels(): List<IncentiveLevelDto>? {
    LOG.trace("Calling incentives-api to get all incentive levels")

    return webClient.get()
      .uri("/incentive/levels")
      .retrieve()
      .bodyToMono<List<IncentiveLevelDto>>()
      .block(apiTimeout)
  }
}
