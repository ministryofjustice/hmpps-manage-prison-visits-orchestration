package uk.gov.justice.digital.hmpps.prison.visits.orchestration.exception

import java.util.function.Supplier

class BookerAuthFailureException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<BookerAuthFailureException> {
  override fun get(): BookerAuthFailureException = BookerAuthFailureException(message, cause)
}
