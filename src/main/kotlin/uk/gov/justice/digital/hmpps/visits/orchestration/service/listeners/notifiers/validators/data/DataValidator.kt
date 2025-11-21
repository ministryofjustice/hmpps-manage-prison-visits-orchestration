package uk.gov.justice.digital.hmpps.visits.orchestration.service.listeners.notifiers.validators.data

interface DataValidator<T> {
  fun isValid(t: T): Boolean
}
