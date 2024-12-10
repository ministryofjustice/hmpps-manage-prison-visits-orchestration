package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.AlertsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.RestPage
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.AlertResponseDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api.enums.PrisonerSupportedAlertCodeType

@Service
class AlertsService(
  private val alertsApiClient: AlertsApiClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val prisonerSupportedAlertCodes = PrisonerSupportedAlertCodeType.entries.map { it.name }.toSet()
  }

  /**
   * Gets all alerts and filters them to only return active. Maps the alert object to only capture the alert codes as a list<String>.
   */
  fun getPrisonerAlertCodes(prisonerId: String): List<AlertResponseDto> {
    logger.info("getPrisonerActiveAlertCodes called for $prisonerId")

    val alerts = alertsApiClient.getPrisonerAlerts(prisonerId)
    return alerts.content.toList().filter { code -> code.alertCode.code in prisonerSupportedAlertCodes }
  }

  fun filterSupportedAlerts(prisonerAlerts: RestPage<AlertResponseDto>): List<AlertResponseDto> {
    return prisonerAlerts.content.toList().filter { code -> code.alertCode.code in prisonerSupportedAlertCodes }
  }
}
