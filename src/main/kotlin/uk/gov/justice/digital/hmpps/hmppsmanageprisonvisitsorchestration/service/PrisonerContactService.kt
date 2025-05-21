package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException

@Service
class PrisonerContactService(
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonersApprovedSocialContactsWithDOB(prisonerNumber: String): List<PrisonerContactDto> {
    LOG.debug("Getting approved social contacts with a DOB for prisoner - {}", prisonerNumber)
    return prisonerContactRegistryClient.getPrisonersApprovedSocialContacts(prisonerNumber, withAddress = false, hasDateOfBirth = true)
  }

  fun getPrisonersContacts(prisonerIds: Set<String>): Map<String, List<PrisonerContactDto>> {
    val prisonersContactMap = mutableMapOf<String, List<PrisonerContactDto>>()
    prisonerIds.forEach { prisonerId ->
      val contacts = try {
        getPrisonerContacts(prisonerId)
      } catch (e: NotFoundException) {
        LOG.info("No contacts found for prisoner id - $prisonerId")
        emptyList()
      }
      prisonersContactMap[prisonerId] = contacts
    }

    return prisonersContactMap.toMap()
  }

  private fun getPrisonerContacts(prisonerId: String): List<PrisonerContactDto> = prisonerContactRegistryClient.getPrisonersSocialContacts(
    prisonerId = prisonerId,
    withAddress = false,
  )
}
