package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.BookerPrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.PublicBookerValidationUtil
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.PublicBookerValidationUtil.Companion.PRISON_VALIDATION_ERROR_MSG
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.VisitBalancesUtil
import java.text.MessageFormat
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

@Component
class PrisonerInfoClient(
  private val prisonApiClient: PrisonApiClient,
  private val prisonService: PrisonService,
  private val visitSchedulerClient: VisitSchedulerClient,
  private val publicBookerValidationUtil: PublicBookerValidationUtil,
  private val visitBalancesUtil: VisitBalancesUtil,
  @Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPermittedPrisonerInfo(
    offenderSearchPrisoner: PrisonerDto,
    bookerPrisoner: PermittedPrisonerForBookerDto,
  ): BookerPrisonerInfoDto? {
    val prisonCode = offenderSearchPrisoner.prisonId!!
    val prisonMono = visitSchedulerClient.getPrisonAsMono(prisonCode)
    val visitBalancesDtoMono = prisonApiClient.getVisitBalancesAsMono(offenderSearchPrisoner.prisonerId)

    Mono.zip(prisonMono, visitBalancesDtoMono).block(apiTimeout).also {
      val prison = it?.t1?.getOrNull()
      val visitBalancesDto = it?.t2?.getOrNull()
      if (prison != null) {
        publicBookerValidationUtil.validatePrison(prison, prisonService)?.let { msg ->
          LOG.error(
            "getPermittedPrisonerInfo - ${
              MessageFormat.format(
                PRISON_VALIDATION_ERROR_MSG,
                prisonCode,
                bookerPrisoner.prisonerId,
                msg,
              )
            }",
          )
        } ?: return BookerPrisonerInfoDto(
          offenderSearchPrisoner,
          visitBalancesUtil.calculateAvailableVos(visitBalancesDto),
          visitBalancesUtil.calculateVoRenewalDate(visitBalancesDto),
        )
      } else {
        LOG.error("getPermittedPrisonerInfo Prison with code - $prisonCode, not found on visit-scheduler")
        return null
      }
    }

    return null
  }
}
