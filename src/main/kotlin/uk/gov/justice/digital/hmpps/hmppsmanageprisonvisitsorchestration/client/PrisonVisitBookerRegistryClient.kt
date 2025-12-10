package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ClientUtils.Companion.isUnprocessableEntityError
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient.Companion.LOG
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerPrisonerValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerVisitorRequestValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AuthDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterVisitorForBookerPrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.VisitorRequestsCountByPrisonCodeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerSearchResultsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.SearchBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.BookerPrisonerRegistrationErrorCodes
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerPrisonerRegistrationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerPrisonerValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.BookerVisitorRequestValidationException
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException
import java.time.Duration

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker"

const val BOOKER_REGISTRY_AUDIT_HISTORY: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}/audit"

const val PERMITTED_PRISONERS: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}/permitted/prisoners"
const val REGISTER_PRISONER: String = "$PERMITTED_PRISONERS/register"
const val VALIDATE_PRISONER: String = "$PERMITTED_PRISONERS/{prisonerId}/validate"

const val PERMITTED_VISITORS: String = "$PERMITTED_PRISONERS/{prisonerId}/permitted/visitors"

// visitor request endpoints
const val ADD_VISITOR_REQUEST: String = "$PERMITTED_VISITORS/request"
const val GET_VISITOR_REQUESTS_BY_BOOKER_REFERENCE: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/{bookerReference}/permitted/visitors/requests"

const val GET_SINGLE_VISITOR_REQUEST = "/visitor-requests/{requestReference}"

const val PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE: String = "/prison/{prisonCode}/visitor-requests/count"
const val PUBLIC_BOOKER_GET_VISITOR_REQUESTS_BY_PRISON_CODE: String = "/prison/{prisonCode}/visitor-requests"

// Admin endpoints
const val BOOKER_ADMIN_ENDPOINT = "$PUBLIC_BOOKER_CONTROLLER_PATH/config"
const val SEARCH_FOR_BOOKER: String = "$BOOKER_ADMIN_ENDPOINT/search"
const val GET_BOOKER_BY_BOOKING_REFERENCE: String = "$BOOKER_ADMIN_ENDPOINT/{bookerReference}"
const val LINK_VISITOR: String = "$BOOKER_ADMIN_ENDPOINT/{bookerReference}/prisoner/{prisonerId}/visitor"
const val UNLINK_VISITOR: String = "$BOOKER_ADMIN_ENDPOINT/{bookerReference}/prisoner/{prisonerId}/visitor/{visitorId}"

@Component
class PrisonVisitBookerRegistryClient(
  @param:Qualifier("prisonVisitBookerRegistryWebClient") private val webClient: WebClient,
  @param:Value("\${prison-visit-booker-registry.api.timeout:10s}") private val apiTimeout: Duration,
  val objectMapper: ObjectMapper,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun bookerAuthorisation(createBookerAuthDetailDto: AuthDetailDto): BookerReference? = webClient.put()
    .uri("/register/auth")
    .body(BodyInserters.fromValue(createBookerAuthDetailDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<BookerReference>().block(apiTimeout)

  fun getPermittedPrisonersForBooker(bookerReference: String): List<PermittedPrisonerForBookerDto> {
    val uri = PERMITTED_PRISONERS.replace("{bookerReference}", bookerReference)
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<List<PermittedPrisonerForBookerDto>>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getPermittedPrisonersForBooker Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getPermittedPrisonersForBooker NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Prisoners for booker reference - $bookerReference not found on public-visits-booker-registry") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { NotFoundException("Prisoners for booker reference - $bookerReference not found on public-visits-booker-registry") }
  }

  fun getPermittedVisitorsForBookersAssociatedPrisoner(bookerReference: String, prisonerNumber: String): List<PermittedVisitorsForPermittedPrisonerBookerDto> {
    val uri = PERMITTED_VISITORS.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerNumber)
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<List<PermittedVisitorsForPermittedPrisonerBookerDto>>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getPermittedVisitorsForBookersAssociatedPrisoner Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getPermittedVisitorsForBookersAssociatedPrisoner NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("Visitors for booker reference - $bookerReference and prisoner id - $prisonerNumber not found on public-visits-booker-registry") }
        }
      }
      .blockOptional(apiTimeout)
      .orElseThrow { NotFoundException("Permitted visitors for booker reference - $bookerReference and prisoner id - $prisonerNumber not found on public-visits-booker-registry") }
  }

  fun validatePrisoner(bookerReference: String, prisonerNumber: String) {
    val uri = VALIDATE_PRISONER.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerNumber)
    webClient.get()
      .uri(uri).retrieve().toBodilessEntity().onErrorResume { e ->
        if (isUnprocessableEntityError(e)) {
          val exception = getPrisonerValidationErrorResponse(e)
          Mono.error(exception)
        } else {
          Mono.error(e)
        }
      }.block(apiTimeout)
  }

  fun registerPrisoner(bookerReference: String, registerPrisonerForBookerDto: RegisterPrisonerForBookerDto) {
    val uri = REGISTER_PRISONER.replace("{bookerReference}", bookerReference)
    webClient.put()
      .uri(uri)
      .body(BodyInserters.fromValue(registerPrisonerForBookerDto))
      .retrieve()
      .toBodilessEntity()
      .onErrorResume { e ->
        if (isUnprocessableEntityError(e)) {
          val exception = getPrisonerRegistrationErrorResponse(e)
          Mono.error(exception)
        } else {
          Mono.error(e)
        }
      }
      .block(apiTimeout)
  }

  fun registerVisitorForBookerPrisoner(bookerReference: String, prisonerId: String, registerVisitorForBookerPrisonerDto: RegisterVisitorForBookerPrisonerDto): PermittedVisitorsForPermittedPrisonerBookerDto {
    val uri = LINK_VISITOR
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)

    return webClient.put()
      .uri(uri)
      .body(BodyInserters.fromValue(registerVisitorForBookerPrisonerDto))
      .retrieve()
      .bodyToMono<PermittedVisitorsForPermittedPrisonerBookerDto>()
      .blockOptional(apiTimeout)
      .orElseThrow { IllegalStateException("Empty response from registerVisitorForBookerPrisoner call with URL $uri") }
  }

  fun searchForBooker(searchBookerDto: SearchBookerDto): List<BookerSearchResultsDto> = webClient.post()
    .uri(SEARCH_FOR_BOOKER)
    .body(BodyInserters.fromValue(searchBookerDto))
    .retrieve()
    .bodyToMono<List<BookerSearchResultsDto>>()
    .onErrorResume { e ->
      if (!ClientUtils.isNotFoundError(e)) {
        logger.error("searchForBooker Failed for request to uri $SEARCH_FOR_BOOKER")
        Mono.error(e)
      } else {
        logger.error("searchForBooker NOT_FOUND for request to uri $SEARCH_FOR_BOOKER")
        Mono.error { NotFoundException("searchForBooker call failed for request to uri $SEARCH_FOR_BOOKER") }
      }
    }
    .blockOptional(apiTimeout).orElseThrow { NotFoundException("searchForBooker call failed for request to uri $SEARCH_FOR_BOOKER") }

  fun getBookerByBookerReference(bookerReference: String): BookerInfoDto {
    val uri = GET_BOOKER_BY_BOOKING_REFERENCE.replace("{bookerReference}", bookerReference)

    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<BookerInfoDto>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getBookerByBookerReference Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getBookerByBookerReference NOT_FOUND for get request $uri")
          Mono.error { NotFoundException("booker not found on booker-registry for booker reference - $bookerReference") }
        }
      }
      .blockOptional(apiTimeout)
      .orElseThrow { NotFoundException("booker not found on booker-registry for booker reference - $bookerReference") }
  }

  fun getBookerAuditHistoryAsMono(bookerReference: String): Mono<List<BookerAuditDto>> = webClient.get()
    .uri(BOOKER_REGISTRY_AUDIT_HISTORY.replace("{bookerReference}", bookerReference))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<List<BookerAuditDto>>()

  fun unlinkBookerPrisonerVisitor(bookerReference: String, prisonerNumber: String, visitorId: String) {
    val uri = UNLINK_VISITOR
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerNumber)
      .replace("{visitorId}", visitorId)

    webClient.delete()
      .uri(uri)
      .retrieve()
      .toBodilessEntity()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("unlinkBookerPrisonerVisitor Failed to complete delete request $uri")
          Mono.error(e)
        } else {
          logger.error("unlinkBookerPrisonerVisitor NOT_FOUND on delete request $uri, returning 200")
          Mono.empty()
        }
      }
      .block(apiTimeout)
  }

  fun createAddVisitorRequest(bookerReference: String, prisonerId: String, addVisitorToBookerPrisonerRequestDto: AddVisitorToBookerPrisonerRequestDto) {
    webClient.post()
      .uri(ADD_VISITOR_REQUEST.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId))
      .body(BodyInserters.fromValue(addVisitorToBookerPrisonerRequestDto))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .toBodilessEntity()
      .onErrorResume { e ->
        if (isUnprocessableEntityError(e)) {
          val exception = getBookerVisitorRequestValidationErrorResponse(e)
          Mono.error(exception)
        } else {
          Mono.error(e)
        }
      }
      .block(apiTimeout)
  }

  fun getActiveVisitorRequestsForBooker(bookerReference: String): List<BookerPrisonerVisitorRequestDto>? {
    val uri = GET_VISITOR_REQUESTS_BY_BOOKER_REFERENCE.replace("{bookerReference}", bookerReference)
    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<BookerPrisonerVisitorRequestDto>>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getActiveVisitorRequestsForBooker Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getActiveVisitorRequestsForBooker NOT_FOUND for get request $uri")
          Mono.empty()
        }
      }
      .block(apiTimeout)
  }

  fun getSingleVisitorRequest(requestReference: String): PrisonVisitorRequestDto {
    val uri = GET_SINGLE_VISITOR_REQUEST.replace("{requestReference}", requestReference)

    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<PrisonVisitorRequestDto>()
      .onErrorResume { e ->
        if (!ClientUtils.isNotFoundError(e)) {
          logger.error("getSingleVisitorRequest Failed for get request $uri")
          Mono.error(e)
        } else {
          logger.error("getSingleVisitorRequest NOT_FOUND for get request $uri")
          Mono.empty()
        }
      }
      .block(apiTimeout) ?: throw NotFoundException("timeout response from prison-visit-booker-registry for getSingleVisitorRequest")
  }

  fun getVisitorRequestsCountByPrisonCode(prisonCode: String): VisitorRequestsCountByPrisonCodeDto {
    val uri = PUBLIC_BOOKER_GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE.replace("{prisonCode}", prisonCode)
    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitorRequestsCountByPrisonCodeDto>()
      .onErrorResume { e ->
        logger.error("getVisitorRequestsCountByPrisonCode Failed for get request $uri")
        Mono.error(e)
      }
      .block(apiTimeout) ?: throw IllegalStateException("timeout response from prison-visit-booker-registry for getVisitorRequestsCountByPrisonCode with code $prisonCode")
  }

  fun getVisitorRequestsByPrisonCode(prisonCode: String): List<PrisonVisitorRequestDto> {
    val uri = PUBLIC_BOOKER_GET_VISITOR_REQUESTS_BY_PRISON_CODE.replace("{prisonCode}", prisonCode)
    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<PrisonVisitorRequestDto>>()
      .onErrorResume { e ->
        logger.error("getVisitorRequestsByPrisonCode Failed for get request $uri")
        Mono.error(e)
      }
      .block(apiTimeout) ?: throw IllegalStateException("timeout response from prison-visit-booker-registry for getVisitorRequestsByPrisonCode with code $prisonCode")
  }

  private fun getPrisonerValidationErrorResponse(e: Throwable): Throwable {
    if (e is WebClientResponseException && isUnprocessableEntityError(e)) {
      try {
        val errorResponse = objectMapper.readValue(e.responseBodyAsString, BookerPrisonerValidationErrorResponse::class.java)
        return BookerPrisonerValidationException(errorResponse.validationError)
      } catch (jsonProcessingException: Exception) {
        LOG.error("An error occurred processing the booker prisoner validation error response - ${e.stackTraceToString()}")
        throw jsonProcessingException
      }
    }

    return e
  }

  private fun getPrisonerRegistrationErrorResponse(e: Throwable): Throwable {
    if (e is WebClientResponseException && isUnprocessableEntityError(e)) {
      try {
        return BookerPrisonerRegistrationException(BookerPrisonerRegistrationErrorCodes.FAILED_REGISTRATION)
      } catch (jsonProcessingException: Exception) {
        LOG.error("An error occurred processing the booker prisoner registration error response - ${e.stackTraceToString()}")
        throw jsonProcessingException
      }
    }

    return e
  }

  private fun getBookerVisitorRequestValidationErrorResponse(e: Throwable): Throwable {
    if (e is WebClientResponseException && isUnprocessableEntityError(e)) {
      try {
        val errorResponse = objectMapper.readValue(e.responseBodyAsString, BookerVisitorRequestValidationErrorResponse::class.java)
        return BookerVisitorRequestValidationException(errorResponse.validationError)
      } catch (jsonProcessingException: Exception) {
        LOG.error("An error occurred submitting an add visitor request, error response - ${e.stackTraceToString()}")
        throw jsonProcessingException
      }
    }

    return e
  }
}
