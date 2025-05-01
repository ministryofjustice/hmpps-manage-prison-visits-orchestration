package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertDto
import java.util.Comparator

class Comparators {
  companion object {
    val alertsComparatorDateUpdatedDescThenByDateCreatedDesc: Comparator<AlertDto> = Comparator.nullsLast(compareByDescending(AlertDto::dateUpdated)).thenComparing(AlertDto::dateCreated)
  }
}
