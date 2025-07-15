package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class CurrentDateUtils {
  fun getCurrentDate(): LocalDate = LocalDate.now()
}
