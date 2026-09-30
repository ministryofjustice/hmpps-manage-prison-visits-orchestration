package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.builder

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonClientType

internal class DefaultPrisonStaffInactiveClientBuilder {
  companion object {
    fun build() = PrisonUserClientDto(
      userType = PrisonClientType.STAFF,
      clientType = PrisonClientType.STAFF,
      policyNoticeDaysMin = 2,
      policyNoticeDaysMax = 28,
      active = false,
    )
  }
}
