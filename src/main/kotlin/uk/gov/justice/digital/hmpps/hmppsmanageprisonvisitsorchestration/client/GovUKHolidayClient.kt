package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.govuk.holidays.HolidaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.GovUkHolidayService
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

@Component
class GovUKHolidayClient(
  @param:Qualifier("govUKWebClient") private val govUKWebClient: WebClient,
  @param:Value("\${gov-uk.api.timeout:10s}") val apiTimeout: Duration,

  @param:Value("\${gov-uk.api.bank-holidays-local-cache}")
  private val localCacheResourceFile: String,

  private val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule()),
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    const val HOLIDAYS_JSON = "bank-holidays.json"
  }

  @Cacheable("bank-holidays", unless = "#result == null")
  fun getHolidays(): HolidaysDto? = govUKWebClient.get().uri(HOLIDAYS_JSON)
    .retrieve().bodyToMono<HolidaysDto>().onErrorResume { e ->
      LOG.error("getHolidays: Error retrieving holidays from gov uk api - ${e.message}")
      Mono.error(e)
    }.block(apiTimeout)

  // TODO - to be discussed if worth storing in cache as needs to be updated manually but might be handy in non-prod envs
  @Cacheable(value = ["bank-holidays-local-cache"], unless = "#result == null")
  fun getHolidaysFromLocalCache(): HolidaysDto? {
    GovUkHolidayService.Companion.LOG.info("Entered getHolidaysFromLocalCache")
    val file: Path = ClassPathResource(localCacheResourceFile).file.toPath()
    val holidaysCacheData = Files.readString(file)
    val holidays = mapper.readValue(holidaysCacheData, HolidaysDto::class.java)
    return holidays
  }
}
