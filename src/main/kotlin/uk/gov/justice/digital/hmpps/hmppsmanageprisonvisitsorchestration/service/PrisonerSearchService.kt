package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto

@Service
class PrisonerSearchService(
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisoner(prisonerNumber: String): PrisonerDto? {
    var prisoner: PrisonerDto? = null

    try {
      prisoner = prisonerSearchClient.getPrisonerById(prisonerNumber)
    } catch (e: WebClientResponseException) {
      logger.info("Failed to get details for prisoner - $prisonerNumber, error = ${e.message}")
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        throw e
      }
    }

    return prisoner
  }
}
