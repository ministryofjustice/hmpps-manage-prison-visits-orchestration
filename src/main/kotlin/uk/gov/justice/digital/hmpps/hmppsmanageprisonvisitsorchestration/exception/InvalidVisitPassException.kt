package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception

import jakarta.validation.ValidationException
import java.util.function.Supplier

class InvalidVisitPassException(message: String? = null, cause: Throwable? = null) :
  ValidationException(message, cause),
  Supplier<InvalidVisitPassException> {
  override fun get(): InvalidVisitPassException = InvalidVisitPassException(message, cause)
}
