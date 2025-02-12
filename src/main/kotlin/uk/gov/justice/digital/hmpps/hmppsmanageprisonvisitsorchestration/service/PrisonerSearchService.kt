package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonerContactService.Companion.LOG

@Service
class PrisonerSearchService(
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisoner(prisonerNumber: String): PrisonerDto = prisonerSearchClient.getPrisonerById(prisonerNumber)

  fun getPrisoners(prisonerIds: Set<String>): Map<String, PrisonerDto?> {
    val prisonerInfoMap = mutableMapOf<String, PrisonerDto?>()
    prisonerIds.forEach { prisonerId ->
      val prisoner = try {
        getPrisoner(prisonerId)
      } catch (e: NotFoundException) {
        LOG.info("No prisoner found for prisoner id - $prisonerId")
        null
      }
      prisonerInfoMap[prisonerId] = prisoner
    }

    return prisonerInfoMap
  }
}
