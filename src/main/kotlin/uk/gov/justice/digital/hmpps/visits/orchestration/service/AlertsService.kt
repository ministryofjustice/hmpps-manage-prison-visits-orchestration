package uk.gov.justice.digital.hmpps.visits.orchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.visits.orchestration.client.AlertsApiClient
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.alerts.api.AlertResponseDto
import uk.gov.justice.digital.hmpps.visits.orchestration.dto.alerts.api.enums.PrisonerSupportedAlertCodeType
import java.util.function.Predicate

@Service
class AlertsService(
  private val alertsApiClient: AlertsApiClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val prisonerSupportedAlertCodes = PrisonerSupportedAlertCodeType.entries.map { it.name }.toSet()
    val predicateFilterSupportedCodes = Predicate<AlertResponseDto> { code -> code.alertCode.code in prisonerSupportedAlertCodes }
  }

  /**
   * Gets all alerts and filters them to only return active. Maps the alert object to only capture the alert codes as a list<String>.
   */
  fun getSupportedPrisonerActiveAlertCodes(prisonerId: String): List<String> {
    logger.info("getPrisonerActiveAlertCodes called for $prisonerId")

    val alerts = alertsApiClient.getPrisonerAlerts(prisonerId)
    return alerts.content.toList().filter { predicateFilterSupportedCodes.test(it) }.map { it.alertCode.code }
  }
}
