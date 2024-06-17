package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.validators

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonService

@Component
@EventValidator(name = "Supported prison validator", description = "Validates if a prison is supported on VSIP.")
class SupportedPrisonValidator(private val prisonService: PrisonService) {
  fun isSupportedPrison(prisonCode: String): Boolean {
    return prisonService.getSupportedPrisons(UserType.STAFF).contains(prisonCode.uppercase())
  }
}
