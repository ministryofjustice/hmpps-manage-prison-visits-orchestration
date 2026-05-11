package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.VisitSchedulerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.ContactRestrictionUpsertedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerAlertNotificationInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.PrisonerContactRestrictionUpsertedInfo

interface IEventNotifier {
  fun process(domainEvent: DomainEvent)
}

abstract class EventNotifier : IEventNotifier {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var visitSchedulerService: VisitSchedulerService

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  final override fun process(domainEvent: DomainEvent) {
    LOG.debug("Entered process for ${this::class.java.name} type: ${domainEvent.eventType}")
    if (this.isProcessableEvent(domainEvent)) {
      this.processEvent(domainEvent)
    }
  }

  fun <T> getAdditionalInfo(domainEvent: DomainEvent, target: Class<T>): T = objectMapper.readValue(domainEvent.additionalInformation, target)

  fun getPrisonerContactRestrictionUpsertedInfo(domainEvent: DomainEvent): PrisonerContactRestrictionUpsertedInfo {
    val prisonerContactRestrictionUpsertedInfo = getAdditionalInfo(domainEvent, PrisonerContactRestrictionUpsertedInfo::class.java)
    val prisonerId = domainEvent.personReference?.identifiers?.firstOrNull { it.type == Identifier.NOMS }?.value
    val contactId = domainEvent.personReference?.identifiers?.firstOrNull { it.type == Identifier.DPS_CONTACT_ID }?.value
    prisonerContactRestrictionUpsertedInfo.prisonerNumber = prisonerId
    prisonerContactRestrictionUpsertedInfo.contactId = contactId

    return prisonerContactRestrictionUpsertedInfo
  }

  fun getContactRestrictionUpsertedInfo(domainEvent: DomainEvent): ContactRestrictionUpsertedInfo {
    val contactRestrictionUpsertedInfo = getAdditionalInfo(domainEvent, ContactRestrictionUpsertedInfo::class.java)
    val contactId = domainEvent.personReference?.identifiers?.firstOrNull { it.type == Identifier.DPS_CONTACT_ID }?.value
    contactRestrictionUpsertedInfo.contactId = contactId

    return contactRestrictionUpsertedInfo
  }

  fun getPrisonerAlertNotificationInfo(domainEvent: DomainEvent): PrisonerAlertNotificationInfo {
    val prisonerAlertUpsertedInfo = getAdditionalInfo(domainEvent, PrisonerAlertNotificationInfo::class.java)
    val prisonerId = domainEvent.personReference?.identifiers?.firstOrNull { it.type == Identifier.NOMS }?.value
    prisonerAlertUpsertedInfo.prisonerNumber = prisonerId
    return prisonerAlertUpsertedInfo
  }

  fun getVisitSchedulerService() = visitSchedulerService

  abstract fun processEvent(domainEvent: DomainEvent)

  abstract fun isProcessableEvent(domainEvent: DomainEvent): Boolean
}
