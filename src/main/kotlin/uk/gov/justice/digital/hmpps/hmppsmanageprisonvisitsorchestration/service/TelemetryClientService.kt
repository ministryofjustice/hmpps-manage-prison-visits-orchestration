package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.config.trackEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class TelemetryClientService(
  private val telemetryClient: TelemetryClient,
) {
  fun trackVisitPassesEvent(prisonCode: String, visitDate: LocalDate, actionedBy: String, totalVisits: Int) {
    telemetryClient.trackEvent(
      "print-visit-passes",
      properties = mapOf(
        "prisonCode" to prisonCode,
        "visitDate" to formatDateToString(visitDate),
        "actionedBy" to actionedBy,
        "totalVisits" to totalVisits.toString(),
      ),
    )
  }

  private fun formatDateToString(date: LocalDate): String = date.format(DateTimeFormatter.ISO_DATE)
}
