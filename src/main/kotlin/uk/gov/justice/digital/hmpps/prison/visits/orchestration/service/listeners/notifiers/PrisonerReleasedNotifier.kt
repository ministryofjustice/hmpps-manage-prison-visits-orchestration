package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.notifiers.validators.events.PrisonerReleasedInfoValidator

const val PRISONER_RELEASED_TYPE = "prison-offender-events.prisoner.released"

@Component(value = PRISONER_RELEASED_TYPE)
class PrisonerReleasedNotifier(val prisonerReleasedInfoValidator: PrisonerReleasedInfoValidator) : EventNotifier() {
  override fun processEvent(domainEvent: DomainEvent) {
    val additionalInfo = getAdditionalInfo(domainEvent, PrisonerReleasedInfo::class.java)
    LOG.debug("Enter PrisonerReleasedInfo Info: {}", additionalInfo)
    getVisitSchedulerService().processPrisonerReleased(additionalInfo)
  }

  override fun isProcessableEvent(domainEvent: DomainEvent): Boolean {
    val additionalInfo = getAdditionalInfo(domainEvent, PrisonerReleasedInfo::class.java)
    return prisonerReleasedInfoValidator.isValid(additionalInfo)
  }
}
