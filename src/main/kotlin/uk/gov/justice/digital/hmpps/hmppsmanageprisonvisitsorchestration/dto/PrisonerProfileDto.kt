package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSummaryDto

data class PrisonerProfileDto(
  @Valid
  @Schema(description = "Prisoner Details", required = true)
  val prisoner: PrisonerDto,

  @Schema(description = "Category description (from list of assessments)", example = "Category C", required = false)
  val category: String?,

  @Schema(description = "Convicted Status", name = "convictedStatus", example = "Convicted", allowableValues = ["Convicted", "Remand"], required = false)
  val convictedStatus: String?,

  @Schema(description = "Incentive level", example = "Standard", required = false)
  val incentiveLevel: String?,

  @Schema(description = "Prisoner alerts", required = false)
  val alerts: List<AlertDto>?,

  @Schema(description = "Balances of visit orders and privileged visit orders", required = false)
  val visitBalances: VisitBalancesDto? = null,

  @Schema(description = "Past and future visits for the prisoner based on configured duration, empty list if no visits.", required = true)
  val visits: List<VisitSummaryDto>,
) {
  constructor(
    prisoner: PrisonerDto,
    inmateDetail: InmateDetailDto,
    visitBalances: VisitBalancesDto?,
    prisonerBookingSummary: PrisonerBookingSummaryDto?,
    visits: List<VisitSummaryDto>,
  ) : this(
    prisoner = prisoner,
    category = inmateDetail.category,
    convictedStatus = prisonerBookingSummary?.convictedStatus,
    incentiveLevel = prisoner.currentIncentive?.level?.description,
    alerts = inmateDetail.alerts,
    visitBalances = visitBalances,
    visits = visits,
  )
}
