package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import javax.validation.ValidationException

@RestControllerAdvice
class OrchestrationExceptionHandler {
  @ExceptionHandler(InvalidPrisonerProfileException::class)
  fun handleInvalidPrisonerProfileException(e: InvalidPrisonerProfileException): ResponseEntity<ErrorResponse?>? {
    log.error("Prisoner profile not found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "Prisoner profile not found: ${e.cause?.message}",
          developerMessage = e.message
        )
      )
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientResponseException(e: WebClientResponseException): ResponseEntity<ByteArray> {
    if (e.statusCode.is4xxClientError) {
      log.debug("Unexpected client exception with message {}", e.message)
    } else {
      log.error("Unexpected server exception", e)
    }

    return ResponseEntity.status(e.rawStatusCode).body(e.responseBodyAsByteArray)
  }

  @ExceptionHandler(WebClientException::class)
  fun handleWebClientException(e: WebClientException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    val error = ErrorResponse(
      status = HttpStatus.INTERNAL_SERVER_ERROR,
      developerMessage = e.message
    )

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.debug("Validation exception: {}", e.message)
    val error = ErrorResponse(
      status = HttpStatus.BAD_REQUEST,
      userMessage = "Validation failure: ${e.cause?.message}",
      developerMessage = e.message
    )

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
