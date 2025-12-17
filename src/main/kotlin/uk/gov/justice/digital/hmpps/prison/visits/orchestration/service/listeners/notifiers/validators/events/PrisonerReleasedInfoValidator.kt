package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.notifiers.validators.events

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo.PrisonerReleasedInfo
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.notifiers.validators.data.SupportedPrisonValidator

@Component
class PrisonerReleasedInfoValidator(private val supportedPrisonValidator: SupportedPrisonValidator) : EventInfoValidator<PrisonerReleasedInfo> {
  override fun isValid(eventInfo: PrisonerReleasedInfo): Boolean = supportedPrisonValidator.isValid(eventInfo.prisonCode)
}
