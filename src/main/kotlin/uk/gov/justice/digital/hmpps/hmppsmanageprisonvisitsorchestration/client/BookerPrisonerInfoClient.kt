package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.BookerPrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.RegisteredPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.VisitBalancesUtil
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

@Component
class BookerPrisonerInfoClient(
  private val prisonApiClient: PrisonApiClient,
  private val prisonRegisterClient: PrisonRegisterClient,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val visitBalancesUtil: VisitBalancesUtil,
  @Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPermittedPrisonerInfo(
    bookerPrisoner: PermittedPrisonerForBookerDto,
  ): BookerPrisonerInfoDto? {
    val prisonCode = bookerPrisoner.prisonCode
    val prisonerId = bookerPrisoner.prisonerId
    val offenderSearchPrisonerDtoMono = prisonerSearchClient.getPrisonerByIdAsMonoEmptyIfNotFound(prisonerId)
    val visitBalancesDtoMono = prisonApiClient.getVisitBalancesAsMono(prisonerId)
    val registeredPrisonMono = prisonRegisterClient.getPrisonAsMonoEmptyIfNotFound(prisonCode)
    Mono.zip(offenderSearchPrisonerDtoMono, visitBalancesDtoMono, registeredPrisonMono).block(apiTimeout).also {
      val offenderSearchPrisoner = it?.t1
      val visitBalancesDto = it?.t2?.getOrNull()
      val registeredPrison = getRegisteredPrison(prisonCode, it?.t3?.getOrNull())

      return if (offenderSearchPrisoner == null) {
        LOG.error("getPermittedPrisonerInfo - prisoner with id - $prisonerId not found on offender search")
        null
      } else {
        return BookerPrisonerInfoDto(
          offenderSearchPrisoner,
          visitBalancesUtil.calculateAvailableVos(visitBalancesDto),
          visitBalancesUtil.calculateVoRenewalDate(visitBalancesDto),
          registeredPrison,
        )
      }
    }
  }

  private fun getRegisteredPrison(prisonCode: String, prisonDto: PrisonRegisterPrisonDto?): RegisteredPrisonDto = if (prisonDto != null) {
    RegisteredPrisonDto(prisonDto)
  } else {
    RegisteredPrisonDto(
      prisonCode = prisonCode,
      prisonName = prisonCode,
    )
  }
}
