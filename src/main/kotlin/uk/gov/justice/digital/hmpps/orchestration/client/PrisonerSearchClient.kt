package uk.gov.justice.digital.hmpps.orchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.orchestration.client.ClientUtils.Companion.isNotFoundError
import uk.gov.justice.digital.hmpps.orchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.orchestration.dto.prisoner.search.AttributeSearchPrisonerDto
import uk.gov.justice.digital.hmpps.orchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.orchestration.exception.NotFoundException
import java.time.Duration

@Component
class PrisonerSearchClient(
  @param:Qualifier("prisonerSearchWebClient") private val webClient: WebClient,
  @param:Value("\${prisoner.search.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val ATTRIBUTE_SEARCH_RESPONSE_FIELDS = listOf("prisonerNumber", "firstName", "lastName")
  }

  fun getPrisonerById(prisonerId: String): PrisonerDto = getPrisonerByIdAsMono(prisonerId)
    .onErrorResume { e ->
      if (!isNotFoundError(e)) {
        logger.error("Failed to get prisoner with id - $prisonerId on prisoner search")
        Mono.error(e)
      } else {
        logger.error("Prisoner with id - $prisonerId not found.")
        Mono.error { NotFoundException("Prisoner with id - $prisonerId not found on prisoner search") }
      }
    }
    .blockOptional(apiTimeout).orElseThrow { NotFoundException("Prisoner with id - $prisonerId not found on prisoner search") }

  fun getPrisonerByIdAsMono(prisonerId: String): Mono<PrisonerDto> = webClient.get().uri("/prisoner/$prisonerId")
    .retrieve()
    .bodyToMono()

  fun getPrisonerByIdAsMonoEmptyIfNotFound(prisonerId: String): Mono<PrisonerDto> = getPrisonerByIdAsMono(prisonerId).onErrorResume { e ->
    if (!isNotFoundError(e)) {
      logger.error("getPrisonerByIdAsMonoEmptyIfNotFound - Failed to get prisoner with id - $prisonerId on prisoner search")
      Mono.error(e)
    } else {
      logger.error("getPrisonerByIdAsMonoEmptyIfNotFound - Prisoner with id - $prisonerId not found.")
      Mono.empty()
    }
  }

  fun getPrisonersByPrisonerIdsAttributeSearch(prisonerIds: List<String>): RestPage<AttributeSearchPrisonerDto>? {
    logger.info("Calling prisoner-search to get all prisoners for given prisonerIds $prisonerIds")
    val responseFields = ATTRIBUTE_SEARCH_RESPONSE_FIELDS.joinToString(separator = ",")

    val requestBody = AttributeSearch(
      queries = listOf(
        AttributeQuery(
          matchers = listOf(
            Matcher(attribute = "prisonerNumber", condition = "IN", searchTerm = prisonerIds.joinToString(separator = ",")),
          ),
        ),
      ),
    )

    return webClient
      .post()
      .uri("/attribute-search?size=10000&responseFields=$responseFields")
      .bodyValue(requestBody)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<RestPage<AttributeSearchPrisonerDto>>()
      .block(apiTimeout)
  }
}

data class AttributeSearch(
  val joinType: String = "AND",
  val queries: List<AttributeQuery>,
)

data class AttributeQuery(
  val joinType: String = "AND",
  val matchers: List<Matcher>,
)

data class Matcher(
  val type: String = "String",
  val attribute: String,
  val condition: String,
  val searchTerm: String,
)
