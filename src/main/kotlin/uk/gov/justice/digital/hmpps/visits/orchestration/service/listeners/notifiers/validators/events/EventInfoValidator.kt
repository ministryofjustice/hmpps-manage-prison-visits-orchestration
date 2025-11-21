package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.notifiers.validators.events

import uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.events.additionalinfo.EventInfo

interface EventInfoValidator<T : EventInfo> {
  fun isValid(eventInfo: T): Boolean
}
