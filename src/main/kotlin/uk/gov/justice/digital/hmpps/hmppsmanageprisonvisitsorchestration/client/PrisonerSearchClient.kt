package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import java.time.Duration

@Component
class PrisonerSearchClient(
  @Qualifier("prisonerSearchWebClient") private val webClient: WebClient,
  @Value("\${prisoner.search.timeout:10s}") private val apiTimeout: Duration,
) {
  fun getPrisonerById(prisonerId: String): PrisonerDto? {
    return getPrisonerByIdAsMono(prisonerId).block(apiTimeout)
  }

  fun getPrisonerByIdAsMono(prisonerId: String): Mono<PrisonerDto> {
    return webClient.get().uri("/prisoner/$prisonerId")
      .retrieve()
      .bodyToMono()
  }

  fun getPrisoners(
    search: String,
    prisonId: String,
    page: Int? = 0,
    size: Int? = 10,
  ): RestPage<PrisonerDto>? {
    return webClient.get().uri("/prison/$prisonId/prisoners") {
      it.queryParam("term", search)
        .queryParam("page", page)
        .queryParam("size", size).build()
    }.retrieve()
      .bodyToMono<RestPage<PrisonerDto>>()
      .block(apiTimeout)
  }

  fun getPrisoner(search: String, prisonId: String): ArrayList<PrisonerDto>? {
    return webClient.get().uri("/prison/$prisonId/prisoners") {
      it.queryParam("term", search).build()
    }.retrieve()
      .bodyToMono<ArrayList<PrisonerDto>>()
      .block(apiTimeout)
  }

  fun getPrisonersByPrisonerNumbers(
    prisonerNumbers: ArrayList<String>,
    page: Int? = 0,
  ): RestPage<String>? {
    return webClient.get().uri("/prisoner-search/prisoner-numbers") {
      it.queryParam("prisonerNumbers", prisonerNumbers).build()
    }.retrieve().bodyToMono<RestPage<String>>()
      .block(apiTimeout)
  }
}
