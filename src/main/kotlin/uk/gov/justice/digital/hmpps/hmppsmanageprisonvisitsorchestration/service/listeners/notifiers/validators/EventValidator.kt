package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.validators

interface EventValidator<T> {
  fun isValid(t: T): Boolean
}
