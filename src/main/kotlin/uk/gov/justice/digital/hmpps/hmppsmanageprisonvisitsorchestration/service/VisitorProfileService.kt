package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.BasicContactDto

@Service
class VisitorProfileService(
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getVisitorsDetails(prisonerId: String, visitorIds: List<Long>): List<BasicContactDto> {
    val contacts = mutableListOf<BasicContactDto>()
    LOG.debug("Entered getVisitorsDetails prisonerId:$prisonerId , visitorIds:{${visitorIds.joinToString(", ")}}")

    val prisonerContacts = prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerId, false)
    visitorIds.forEach { visitorId ->
      prisonerContacts?.firstOrNull { it.personId == visitorId }?.let { contact ->
        contacts.add(BasicContactDto(contact))
      }
    }

    return contacts
  }
}
