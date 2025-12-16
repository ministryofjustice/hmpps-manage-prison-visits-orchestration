package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonVisitBookerRegistryClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.ApproveVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PrisonVisitorRequestListEntryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RejectVisitorRequestDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.SingleVisitorRequestForReviewDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.VisitorRequestsCountByPrisonCodeDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.AttributeSearchPrisonerDto

@Service
class PublicBookerVisitorRequestsService(
  private val prisonVisitBookerRegistryClient: PrisonVisitBookerRegistryClient,
  private val publicBookerService: PublicBookerService,
  private val prisonerSearchClient: PrisonerSearchClient,
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
    val prisonerInfo = prisonerSearchClient.getPrisonerById(visitorRequest.prisonerId)

    return SingleVisitorRequestForReviewDto(visitorRequest, prisonerInfo, socialContacts)
  }

  fun getCountVisitorRequestsForPrison(prisonCode: String): VisitorRequestsCountByPrisonCodeDto {
    LOG.info("Entered PublicBookerVisitorRequestsService - getCountVisitorRequestsForPrison - for prison $prisonCode")
    return prisonVisitBookerRegistryClient.getVisitorRequestsCountByPrisonCode(prisonCode)
  }

  fun getVisitorRequestsForPrison(prisonCode: String): List<PrisonVisitorRequestListEntryDto> {
    LOG.info("Entered PublicBookerVisitorRequestsService - getVisitorRequestsForPrison - for prison $prisonCode")
    val requests = prisonVisitBookerRegistryClient.getVisitorRequestsByPrisonCode(prisonCode)

    var prisonerDetailsList = emptyList<AttributeSearchPrisonerDto>()
    try {
      prisonerDetailsList = prisonerSearchClient.getPrisonersByPrisonerIdsAttributeSearch(requests.map { it.prisonerId }.distinct())?.toList() ?: emptyList()
    } catch (e: Exception) {
      LOG.error("Unable to load prisoner details - exception - $e")
    }

    val requestList: MutableList<PrisonVisitorRequestListEntryDto> = mutableListOf()
    requests.forEach { request ->
      val prisonerDetails = prisonerDetailsList.firstOrNull { request.prisonerId == it.prisonerNumber }

      requestList.add(PrisonVisitorRequestListEntryDto(request, prisonerDetails))
    }

    return requestList
  }

  fun approveAndLinkVisitorRequest(requestReference: String, approveVisitorRequestDto: ApproveVisitorRequestDto): PrisonVisitorRequestDto = prisonVisitBookerRegistryClient.approveAndLinkVisitorRequest(requestReference, approveVisitorRequestDto)

  fun rejectVisitorRequest(requestReference: String, rejectVisitorRequestDto: RejectVisitorRequestDto): PrisonVisitorRequestDto = prisonVisitBookerRegistryClient.rejectVisitorRequest(requestReference, rejectVisitorRequestDto)
}
