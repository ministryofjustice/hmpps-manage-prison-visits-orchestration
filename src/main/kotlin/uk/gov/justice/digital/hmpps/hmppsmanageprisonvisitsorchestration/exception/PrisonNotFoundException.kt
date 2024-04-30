package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception

import jakarta.validation.ValidationException
import java.util.function.Supplier

class PrisonNotFoundException(message: String? = null, cause: Throwable? = null) :
  ValidationException(message, cause),
  Supplier<PrisonNotFoundException> {
  override fun get(): PrisonNotFoundException {
    return PrisonNotFoundException(message, cause)
  }
}
