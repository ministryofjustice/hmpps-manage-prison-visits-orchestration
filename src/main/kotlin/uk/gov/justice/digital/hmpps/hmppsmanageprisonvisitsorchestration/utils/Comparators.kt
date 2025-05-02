package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto
import java.util.Comparator

class Comparators {
  companion object {
    val alertsComparatorDateUpdatedOrCreatedDateDesc: Comparator<AlertDto> =
      Comparator { alert1: AlertDto, alert2: AlertDto ->
        val alert1LastUpdatedOrCreatedDate = alert1.dateUpdated ?: alert1.dateCreated
        val alert2LastUpdatedOrCreatedDate = alert2.dateUpdated ?: alert2.dateCreated
        if (alert1 == alert2) {
          0
        } else if (alert1LastUpdatedOrCreatedDate > alert2LastUpdatedOrCreatedDate) {
          -1
        } else {
          1
        }
      }

    val restrictionsComparatorDatCreatedDesc: Comparator<OffenderRestrictionDto> = compareByDescending { it.startDate }
  }
}
