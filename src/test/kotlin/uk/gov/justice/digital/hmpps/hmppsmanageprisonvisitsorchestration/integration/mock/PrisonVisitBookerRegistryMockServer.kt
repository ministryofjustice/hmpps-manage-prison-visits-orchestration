package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.ADD_VISITOR_REQUEST
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.GET_BOOKER_BY_BOOKING_REFERENCE
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.LINK_VISITOR
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PERMITTED_PRISONERS
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PERMITTED_VISITORS
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.REGISTER_PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.SEARCH_FOR_BOOKER
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.UNLINK_VISITOR
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VALIDATE_PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerPrisonerRegistrationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.BookerPrisonerValidationErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerAuditDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerReference
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedVisitorsForPermittedPrisonerBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisterVisitorForBookerPrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.BookerSearchResultsDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin.SearchBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.integration.mock.MockUtils.Companion.getJsonString

class PrisonVisitBookerRegistryMockServer : WireMockServer(8098) {

  fun stubBookerAuthorisation(bookerReference: BookerReference, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      WireMock.put("/register/auth")
        .willReturn(
          responseBuilder.withStatus(httpStatus.value())
            .withBody(getJsonString(bookerReference)),
        ),
    )
  }

  fun stubGetBookersPrisoners(bookerReference: String, bookerPrisoners: List<PermittedPrisonerForBookerDto>?, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get(PERMITTED_PRISONERS.replace("{bookerReference}", bookerReference) + "?active=true")
        .willReturn(
          if (bookerPrisoners == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(bookerPrisoners))
          },
        ),
    )
  }

  fun stubSearchBooker(searchBookerDto: SearchBookerDto, bookers: List<BookerSearchResultsDto>?, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.post(SEARCH_FOR_BOOKER)
        .withRequestBody(equalToJson(getJsonString(searchBookerDto)))
        .willReturn(
          if (bookers == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(bookers))
          },
        ),
    )
  }

  fun stubGetBookerByBookerReference(bookerReference: String, booker: BookerInfoDto?, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get(GET_BOOKER_BY_BOOKING_REFERENCE.replace("{bookerReference}", bookerReference))
        .willReturn(
          if (booker == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(booker))
          },
        ),
    )
  }

  fun stubGetBookersPrisonerVisitors(bookerReference: String, prisonerNumber: String, visitors: List<PermittedVisitorsForPermittedPrisonerBookerDto>?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get(PERMITTED_VISITORS.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerNumber) + "?active=true")
        .willReturn(
          if (visitors == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitors))
          },
        ),
    )
  }

  fun stubValidateBookerPrisoner(bookerReference: String, prisonerNumber: String, httpStatus: HttpStatus) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get(VALIDATE_PRISONER.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerNumber))
        .willReturn(
          responseBuilder
            .withStatus(httpStatus.value()),
        ),
    )
  }

  fun stubPrisonerValidationFailure(bookerReference: String, prisonerNumber: String, errorResponse: BookerPrisonerValidationErrorResponse) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get(VALIDATE_PRISONER.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerNumber))
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .withBody(getJsonString(errorResponse)),
        ),
    )
  }

  fun stubRegisterPrisonerForBooker(bookerReference: String, httpStatus: HttpStatus, errorResponse: BookerPrisonerRegistrationErrorResponse? = null) {
    val responseBuilder = createJsonResponseBuilder().withStatus(httpStatus.value())

    errorResponse?.let {
      responseBuilder.withBody(getJsonString(it))
    }

    stubFor(
      WireMock.put(REGISTER_PRISONER.replace("{bookerReference}", bookerReference))
        .willReturn(responseBuilder),
    )
  }

  fun stubRegisterVisitorForBookerPrisoner(bookerReference: String, prisonerId: String, registerVisitorForBookerPrisonerDto: RegisterVisitorForBookerPrisonerDto, visitorsForPermittedPrisonerBookerDto: PermittedVisitorsForPermittedPrisonerBookerDto, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()
    val uri = LINK_VISITOR.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId)
    stubFor(
      WireMock.post(uri)
        .withRequestBody(equalToJson(getJsonString(registerVisitorForBookerPrisonerDto)))
        .willReturn(
          responseBuilder
            .withStatus(httpStatus.value())
            .withBody(getJsonString(visitorsForPermittedPrisonerBookerDto)),
        ),
    )
  }

  fun stubGetBookerAuditHistory(
    bookerReference: String,
    events: List<BookerAuditDto>? = null,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      WireMock.get("/public/booker/$bookerReference/audit")
        .willReturn(
          if (events != null) {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(events))
          } else {
            responseBuilder.withStatus(httpStatus.value())
          },
        ),
    )
  }

  fun stubUnlinkVisitor(bookerReference: String, prisonerId: String, visitorId: String, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()

    val uri = UNLINK_VISITOR
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)
      .replace("{visitorId}", visitorId)

    stubFor(
      WireMock.delete(uri)
        .willReturn(
          responseBuilder
            .withStatus(httpStatus.value()),
        ),
    )
  }

  fun stubAddVisitorRequest(bookerReference: String, prisonerId: String, addVisitorToBookerPrisonerRequestDto: AddVisitorToBookerPrisonerRequestDto, httpStatus: HttpStatus = HttpStatus.OK) {
    val responseBuilder = createJsonResponseBuilder()
    val uri = ADD_VISITOR_REQUEST.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId)
    stubFor(
      WireMock.post(uri)
        .withRequestBody(equalToJson(getJsonString(addVisitorToBookerPrisonerRequestDto)))
        .willReturn(
          responseBuilder
            .withStatus(httpStatus.value()),
        ),
    )
  }
}
