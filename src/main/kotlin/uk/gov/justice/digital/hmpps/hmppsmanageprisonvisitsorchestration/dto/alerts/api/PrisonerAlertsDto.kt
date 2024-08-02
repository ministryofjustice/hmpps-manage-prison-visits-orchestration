package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.alerts.api

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.AlertDto

/**
 * Prisoner Alerts - retrieved from Alerts API.
 */
@Schema(description = "Prisoner Alerts")
data class PrisonerAlertsDto(
  @Schema(description = "List of alert details", required = true)
  val alerts: List<AlertDto>? = null,
)
