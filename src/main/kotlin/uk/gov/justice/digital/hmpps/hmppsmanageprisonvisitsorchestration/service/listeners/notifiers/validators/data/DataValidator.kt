package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.validators.data

interface DataValidator<T> {
  fun isValid(t: T): Boolean
}
