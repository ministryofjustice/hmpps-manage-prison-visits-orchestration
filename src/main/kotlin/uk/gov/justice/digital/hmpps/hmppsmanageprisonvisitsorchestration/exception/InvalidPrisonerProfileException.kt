package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception

import java.util.function.Supplier

class InvalidPrisonerProfileException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<InvalidPrisonerProfileException> {
  override fun get(): InvalidPrisonerProfileException = InvalidPrisonerProfileException(message, cause)
}
