package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerInfoDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PublicBookerService
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PublicBookerService.Companion.validatePrison
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.VisitBalancesUtil.Companion.calculateAvailableVos
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.VisitBalancesUtil.Companion.calculateVoRenewalDate
import java.text.MessageFormat
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

@Component
class PrisonerInfoClient(
  private val prisonApiClient: PrisonApiClient,
  private val prisonService: PrisonService,
  private val visitSchedulerClient: VisitSchedulerClient,
  @Value("\${prisoner.profile.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPermittedPrisonerInfo(offenderSearchPrisoner: PrisonerDto, bookerPrisoner: PermittedPrisonerForBookerDto): PrisonerInfoDto? {
    val prisonCode = offenderSearchPrisoner.prisonId!!
    val prisonMono = visitSchedulerClient.getPrisonAsMono(prisonCode)
    val visitBalancesDtoMono = prisonApiClient.getVisitBalancesAsMono(offenderSearchPrisoner.prisonerNumber)

    Mono.zip(prisonMono, visitBalancesDtoMono).block(apiTimeout).also {
      val prison = it?.t1?.getOrNull()
      val visitBalancesDto = it?.t2?.getOrNull()
      if (prison != null) {
        validatePrison(prison, prisonService)?.let { msg ->
          LOG.error("getPermittedPrisonerInfo - ${MessageFormat.format(PublicBookerService.PRISON_VALIDATION_ERROR_MSG, prisonCode, bookerPrisoner.prisonerId, msg)}")
        } ?: return PrisonerInfoDto(
          bookerPrisoner.prisonerId,
          offenderSearchPrisoner,
          calculateAvailableVos(visitBalancesDto),
          calculateVoRenewalDate(visitBalancesDto),
        )
      } else {
        LOG.error("getPermittedPrisonerInfo Prison with code - $prisonCode, not found on visit-scheduler")
        return null
      }
    }

    return null
  }
}
