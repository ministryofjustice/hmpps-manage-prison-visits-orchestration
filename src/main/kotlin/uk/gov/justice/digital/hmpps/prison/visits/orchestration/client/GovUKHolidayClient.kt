package uk.gov.justice.digital.hmpps.prison.visits.orchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.govuk.holidays.HolidaysDto
import java.time.Duration

@Component
class GovUKHolidayClient(
  @param:Qualifier("govUKWebClient") private val govUKWebClient: WebClient,
  @param:Value("\${gov-uk.api.timeout:10s}") val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val HOLIDAYS_JSON = "/bank-holidays.json"
  }

  @Cacheable("bank-holidays", unless = "#result == null")
  fun getHolidays(): HolidaysDto? = govUKWebClient.get().uri(HOLIDAYS_JSON)
    .retrieve().bodyToMono<HolidaysDto>().onErrorResume { e ->
      LOG.error("getHolidays: Error retrieving holidays from gov uk api - ${e.message}")
      Mono.error(e)
    }.block(apiTimeout)
}
