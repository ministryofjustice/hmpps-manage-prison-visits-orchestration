package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import java.time.LocalDate

@Service
class PrisonerContactService(
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonersSocialContactsWithDOBAndNotBannedBeforeDate(prisonerNumber: String, notBannedBeforeDate: LocalDate): List<PrisonerContactDto> {
    LOG.debug("Getting approved social contacts with a DOB for prisoner - {}, notBannedBeforeDate - {}", prisonerNumber, notBannedBeforeDate)
    return prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerNumber, withAddress = false, hasDateOfBirth = true, notBannedBeforeDate = notBannedBeforeDate)
  }
}
