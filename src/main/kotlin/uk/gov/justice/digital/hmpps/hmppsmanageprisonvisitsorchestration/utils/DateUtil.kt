package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DateUtil {
  fun getCurrentDate(): LocalDate {
    return LocalDate.now()
  }
}
