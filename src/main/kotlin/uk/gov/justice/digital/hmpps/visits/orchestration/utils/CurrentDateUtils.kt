package uk.gov.justice.digital.hmpps.visits.orchestration.utils

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class CurrentDateUtils {
  fun getCurrentDate(): LocalDate = LocalDate.now()
}
