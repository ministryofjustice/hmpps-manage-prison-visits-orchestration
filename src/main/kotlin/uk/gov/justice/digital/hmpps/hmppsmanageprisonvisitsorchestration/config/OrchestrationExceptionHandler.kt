package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationValidationErrorCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.BookerPrisonerValidationErrorCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.ApplicationValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerAuthFailureException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerPrisonerValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.InvalidPrisonerProfileException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException

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
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NotFoundException::class)
  fun handleNotFoundException(e: NotFoundException): ResponseEntity<ErrorResponse?>? {
    log.error("Not Found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "not found: ${e.cause?.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(BookerAuthFailureException::class)
  fun handleBookerAuthFailureException(e: BookerAuthFailureException): ResponseEntity<ErrorResponse?>? {
    log.error("Booker auth failure exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "Booker auth failure : ${e.cause?.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned with message {}", e.message)
    val error = ErrorResponse(
      status = HttpStatus.FORBIDDEN,
      userMessage = "Access denied",
    )
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientResponseException(e: WebClientResponseException): ResponseEntity<ByteArray> {
    if (e.statusCode.is4xxClientError) {
      log.debug("Unexpected client exception with message {}", e.message)
    } else {
      log.error("Unexpected server exception", e)
    }

    return ResponseEntity.status(e.statusCode).body(e.responseBodyAsByteArray)
  }

  @ExceptionHandler(WebClientException::class)
  fun handleWebClientException(e: WebClientException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    val error = ErrorResponse(
      status = HttpStatus.INTERNAL_SERVER_ERROR,
      developerMessage = e.message,
    )

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.debug("Validation exception: {}", e.message)
    val error = ErrorResponse(
      status = HttpStatus.BAD_REQUEST,
      userMessage = "Validation failure: ${e.cause?.message}",
      developerMessage = e.message,
    )

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
  }

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> {
    log.debug("Handler method validation exception: {}", e.message)
    val message = e.localizedMessage
    val error = ErrorResponse(
      status = HttpStatus.BAD_REQUEST,
      userMessage = message,
      developerMessage = e.message,
    )

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
  }

  @ExceptionHandler(ApplicationValidationException::class)
  fun handleApplicationValidationException(e: ApplicationValidationException): ResponseEntity<ValidationErrorResponse> {
    log.debug("Application Validation exception: {}, {}", e.message, e.errorCodes)
    val message = e.localizedMessage
    val error = ApplicationValidationErrorResponse(
      status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
      userMessage = "Application validation failed",
      developerMessage = message,
      validationErrors = e.errorCodes,
    )

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error)
  }

  @ExceptionHandler(BookerPrisonerValidationException::class)
  fun handleBookerPrisonerValidationException(e: BookerPrisonerValidationException): ResponseEntity<ValidationErrorResponse> {
    log.debug("Prisoner Validation exception: {}, {}", e.message, e.errorCode)
    val message = e.localizedMessage
    val error = BookerPrisonerValidationErrorResponse(
      status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
      userMessage = "Prisoner validation failed",
      developerMessage = message,
      validationError = e.errorCode,
    )

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage)
}

open class ValidationErrorResponse(
  open val status: Int,
  open val errorCode: Int? = null,
  open val userMessage: String? = null,
  open val developerMessage: String? = null,
)

data class ApplicationValidationErrorResponse(
  override val status: Int,
  override val errorCode: Int? = null,
  override val userMessage: String? = null,
  override val developerMessage: String? = null,
  val validationErrors: List<ApplicationValidationErrorCodes>,
) : ValidationErrorResponse(status, errorCode, userMessage, developerMessage)

data class BookerPrisonerValidationErrorResponse(
  override val status: Int,
  override val errorCode: Int? = null,
  override val userMessage: String? = null,
  override val developerMessage: String? = null,
  val validationError: BookerPrisonerValidationErrorCodes,
) : ValidationErrorResponse(status, errorCode, userMessage, developerMessage)
