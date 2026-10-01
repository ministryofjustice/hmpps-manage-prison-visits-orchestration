package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration.builder

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.PrisonClientType

/**
 * Creates an inactive STAFF client - with min 2 and max 28 days.
 */
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
