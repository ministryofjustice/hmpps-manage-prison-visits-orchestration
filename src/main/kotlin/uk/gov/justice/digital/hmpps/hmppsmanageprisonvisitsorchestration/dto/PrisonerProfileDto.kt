package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.AlertDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.PrisonerBookingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api.VisitBalancesDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSummaryDto
import java.time.LocalDate

data class PrisonerProfileDto(
  private val prisoner: PrisonerDto,

  @Schema(description = "Category description (from list of assessments)", example = "Category C")
  val category: String?,

  @Schema(
    description = "Convicted Status",
    name = "convictedStatus",
    example = "Convicted",
    allowableValues = ["Convicted", "Remand"],
  )
  val convictedStatus: String?,

  @Schema(description = "Incentive level", example = "Standard")
  val incentiveLevel: String?,

  @Schema(description = "Alert")
  val alerts: List<AlertDto>?,

  @Schema(description = "Balances of visit orders and privilege visit orders")
  val visitBalances: VisitBalancesDto? = null,

  @Schema(description = "Past and future visits for the prisoner based on configured duration.")
  val visits: List<VisitSummaryDto>,
) : PrisonerDto(
  prisonerId = prisoner.prisonerId,
  prisonId = prisoner.prisonId,
  firstName = prisoner.firstName,
  lastName = prisoner.lastName,
  dateOfBirth = prisoner.dateOfBirth,
  prisonName = prisoner.prisonName,
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

  @JsonCreator
  constructor(
    prisonerId: String,
    prisonId: String?,
    firstName: String,
    lastName: String,
    dateOfBirth: LocalDate,
    cellLocation: String?,
    prisonName: String?,
    category: String?,
    convictedStatus: String?,
    incentiveLevel: String?,
    alerts: List<AlertDto>?,
    visitBalances: VisitBalancesDto? = null,
    visits: List<VisitSummaryDto>,
  ) : this(
    prisoner = PrisonerDto(
      prisonerId = prisonerId,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      prisonId = prisonId,
      cellLocation = cellLocation,
      prisonName = prisonName,
    ),
    category = category,
    convictedStatus = convictedStatus,
    incentiveLevel = incentiveLevel,
    alerts = alerts,
    visitBalances = visitBalances,
    visits = visits,
  )
}
