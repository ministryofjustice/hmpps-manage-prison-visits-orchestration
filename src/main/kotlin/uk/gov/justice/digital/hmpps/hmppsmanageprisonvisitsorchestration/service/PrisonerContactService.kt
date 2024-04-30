package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.RestrictionDto
import java.time.LocalDate

@Service
class PrisonerContactService(
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getAllPrisonersSocialContacts(prisonerNumber: String): List<PrisonerContactDto>? {
    LOG.debug("Getting contacts for prisoner - {}", prisonerNumber)
    var contacts: List<PrisonerContactDto>? = null

    try {
      contacts = prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerNumber, false)
    } catch (e: WebClientResponseException) {
      PublicBookerService.logger.info("Failed to get social contacts for prisoner - $prisonerNumber, error = ${e.message}")
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        throw e
      }
    }

    return contacts
  }

  fun getPrisonersSocialContactByVisitorId(prisonerNumber: String, visitorId: Long): PrisonerContactDto? {
    LOG.debug("Getting contact details - {} with visitorId - {}", prisonerNumber, visitorId)
    val contacts = prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerNumber, false)
    return contacts?.let {
      getContact(contacts, visitorId)
    }
  }

  fun getPrisonersSocialContactByVisitorIds(prisonerNumber: String, visitorIds: List<Long>): List<PrisonerContactDto> {
    val visitorsList = mutableListOf<PrisonerContactDto>()
    LOG.debug("Getting contact details - {} with visitorIds - {}", prisonerNumber, visitorIds)
    val contacts = prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerNumber, false)
    visitorIds.forEach { visitorId ->
      contacts?.firstOrNull{it.personId == visitorId}?.let {
        visitorsList.add(it)
      }
    }

    return visitorsList
  }


  fun isContactBannedBeforeDate(contact: PrisonerContactDto, date: LocalDate): Boolean {
    return contact.restrictions.any {
      hasBanForDate(it, date)
    }
  }

  private fun getContact(contacts: List<PrisonerContactDto>, visitorId: Long): PrisonerContactDto? {
    return contacts.firstOrNull{it.personId == visitorId && it.approvedVisitor}
  }

  private fun hasBanForDate(restriction: RestrictionDto, date: LocalDate): Boolean {
    return (
      restriction.restrictionType == PublicBookerService.BANNED_RESTRICTION_TYPE
        && (restriction.expiryDate == null || restriction.expiryDate.isAfter(date)
      )
    )
  }
}
