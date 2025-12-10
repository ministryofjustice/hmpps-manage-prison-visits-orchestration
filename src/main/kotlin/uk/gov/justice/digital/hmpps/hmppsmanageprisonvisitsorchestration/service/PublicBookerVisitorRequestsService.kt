package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.SingleVisitorRequestForReviewDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.VisitorRequestsCountByPrisonCodeDto

@Service
class PublicBookerVisitorRequestsService(
  private val prisonVisitBookerRegistryClient: PrisonVisitBookerRegistryClient,
  private val publicBookerService: PublicBookerService,
) {
  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createAddVisitorRequest(bookerReference: String, prisonerNumber: String, addVisitorToBookerPrisonerRequestDto: AddVisitorToBookerPrisonerRequestDto) {
    LOG.info("Entered PublicBookerVisitorRequestsService - addVisitorToBookerPrisoner - for booker {}, prisoner {}, addVisitorToBookerRequestDto {}", bookerReference, prisonerNumber, addVisitorToBookerPrisonerRequestDto)
    prisonVisitBookerRegistryClient.createAddVisitorRequest(bookerReference, prisonerNumber, addVisitorToBookerPrisonerRequestDto)
  }

  fun getActiveVisitorRequestsForBooker(bookerReference: String): List<BookerPrisonerVisitorRequestDto> {
    LOG.info("Entered PublicBookerVisitorRequestsService - getActiveVisitorRequestsForBooker - for booker $bookerReference")
    return prisonVisitBookerRegistryClient.getActiveVisitorRequestsForBooker(bookerReference) ?: emptyList()
  }

  fun getVisitorRequestForReview(requestReference: String): SingleVisitorRequestForReviewDto {
    LOG.info("Entered PublicBookerVisitorRequestsService - getVisitorRequestForReview - requestReference $requestReference")

    val visitorRequest = prisonVisitBookerRegistryClient.getSingleVisitorRequest(requestReference)
    val socialContacts = publicBookerService.getSocialContacts(visitorRequest.bookerReference, visitorRequest.prisonerId)

    return SingleVisitorRequestForReviewDto(visitorRequest, socialContacts)
  }

  fun getCountVisitorRequestsForPrison(prisonCode: String): VisitorRequestsCountByPrisonCodeDto {
    LOG.info("Entered PublicBookerVisitorRequestsService - getCountVisitorRequestsForPrison - for prison $prisonCode")
    return prisonVisitBookerRegistryClient.getVisitorRequestsCountByPrisonCode(prisonCode)
  }

  fun getVisitorRequestsForPrison(prisonCode: String): List<PrisonVisitorRequestDto> {
    LOG.info("Entered PublicBookerVisitorRequestsService - getVisitorRequestsForPrison - for prison $prisonCode")
    return prisonVisitBookerRegistryClient.getVisitorRequestsByPrisonCode(prisonCode)
  }
}
