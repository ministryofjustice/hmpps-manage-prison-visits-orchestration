package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.AlertsApiClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.exception.NotFoundException

@Service
class AlertsService(
  private val alertsApiClient: AlertsApiClient,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Gets all alerts and filters them to only return active. Maps the alert object to only capture the alert codes as a list<String>.
   */
  fun getPrisonerActiveAlertCodes(prisonerId: String): List<String> {
    val prisonerAlerts = alertsApiClient.getPrisonerAlerts(prisonerId)
    logger.info("Retrieved $prisonerAlerts alerts for $prisonerId")
    prisonerAlerts?.let {
      return prisonerAlerts.content.filter { it.active }.map { it.alertCode.code }.toList()
    } ?: throw NotFoundException("getPrisonerActiveAlertCodes - alertsApiClient failed to get alerts for prisoner $prisonerId")
  }
}
