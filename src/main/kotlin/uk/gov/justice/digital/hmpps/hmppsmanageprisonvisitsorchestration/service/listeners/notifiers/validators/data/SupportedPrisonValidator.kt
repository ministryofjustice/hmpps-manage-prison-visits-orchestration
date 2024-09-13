package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.validators.data

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonService

@Component
class SupportedPrisonValidator(private val prisonService: PrisonService) : DataValidator<String> {
  private fun isSupportedPrison(prisonCode: String): Boolean {
    return prisonService.getSupportedPrisons(UserType.STAFF).contains(prisonCode.uppercase())
  }

  override fun isValid(t: String): Boolean {
    return isSupportedPrison(t)
  }
}
