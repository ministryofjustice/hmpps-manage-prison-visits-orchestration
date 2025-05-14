package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.OffenderRestrictionDto

class Comparators {
  companion object {
    val alertsComparatorDateUpdatedOrCreatedDateDesc: Comparator<AlertResponseDto> =
      Comparator { alert1: AlertResponseDto, alert2: AlertResponseDto ->
        val alert1LastUpdatedOrCreatedDate = alert1.lastModifiedAt ?: alert1.createdAt
        val alert2LastUpdatedOrCreatedDate = alert2.lastModifiedAt ?: alert2.createdAt
        if (alert1 == alert2) {
          0
        } else if (alert1LastUpdatedOrCreatedDate > alert2LastUpdatedOrCreatedDate) {
          -1
        } else {
          1
        }
      }.thenByDescending { it.activeFrom }
        .thenComparing(AlertResponseDto::activeTo, nullsFirst(reverseOrder()))

    val restrictionsComparatorDatCreatedDesc: Comparator<OffenderRestrictionDto> = compareByDescending<OffenderRestrictionDto> { it.startDate }.thenByDescending { it.expiryDate }
  }
}
