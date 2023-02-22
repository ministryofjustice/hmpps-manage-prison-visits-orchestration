package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException

@RestControllerAdvice
class OrchestrationExceptionHandler {
  @ExceptionHandler(InvalidPrisonerProfileException::class)
  fun handleInvalidPrisonerProfileException(e: InvalidPrisonerProfileException): ResponseEntity<ErrorResponse?>? {
    log.debug("Visit not found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "Visit not found: ${e.cause?.message}",
          developerMessage = e.message
        )
      )
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
