package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
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

  fun getPrisonersSocialContactsWithDOBAndNotBannedBeforeDate(prisonerNumber: String, notBannedBeforeDate: LocalDate): List<PrisonerContactDto>? {
    LOG.debug("Getting approved social contacts with a DOB for prisoner - {}, notBannedBeforeDate - {}", prisonerNumber, notBannedBeforeDate)
    var contacts: List<PrisonerContactDto>? = null

    try {
      contacts = prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerNumber, withAddress = false, hasDateOfBirth = true, notBannedBeforeDate = notBannedBeforeDate)
    } catch (e: WebClientResponseException) {
      LOG.info("Failed to get social contacts for prisoner - $prisonerNumber, error = ${e.message}")
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        throw e
      }
    }

    return contacts
  }
}