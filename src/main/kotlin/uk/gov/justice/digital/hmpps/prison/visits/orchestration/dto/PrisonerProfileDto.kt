package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.alerts.api.AlertDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.orchestration.VisitBalancesDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prison.api.InmateDetailDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prison.api.OffenderRestrictionDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.VisitSummaryDto
import java.time.LocalDate

data class PrisonerProfileDto(
  @param:Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerId: String,

  @param:Schema(description = "Prison ID", example = "MDI")
  val prisonId: String?,

  @param:Schema(required = true, description = "First Name", example = "Robert")
  val firstName: String,

  @param:Schema(required = true, description = "Last name", example = "Larsen")
  val lastName: String,

  @param:Schema(required = false, description = "Date of Birth", example = "1975-04-02")
  val dateOfBirth: LocalDate,

  @param:Schema(description = "In prison cell location", example = "A-1-002")
  val cellLocation: String?,

  @param:Schema(description = "Prison Name", example = "HMP Leeds")
  val prisonName: String?,

  @param:Schema(description = "Category description (from list of assessments)", example = "Category C")
  val category: String?,

  @param:Schema(
    description = "Convicted Status",
    name = "convictedStatus",
    example = "Convicted",
    allowableValues = ["Convicted", "Remand"],
  )
  val convictedStatus: String?,

  @param:Schema(description = "Incentive level", example = "Standard")
  val incentiveLevel: String?,

  @param:Schema(description = "Alert")
  val alerts: List<AlertDto>?,

  @param:Schema(description = "Balances of visit orders and privilege visit orders")
  val visitBalances: VisitBalancesDto? = null,

  @param:Schema(description = "Past and future visits for the prisoner based on configured duration.")
  val visits: List<VisitSummaryDto>,

  @param:Schema(description = "Prisoner restrictions")
  val prisonerRestrictions: List<OffenderRestrictionDto>,
) {
  constructor(
    prisoner: PrisonerDto,
    inmateDetail: InmateDetailDto,
    visitBalances: VisitBalancesDto?,
    visits: List<VisitSummaryDto>,
    prisonerAlerts: List<AlertDto>,
    prisonerRestrictions: List<OffenderRestrictionDto>,
  ) : this(
    prisonerId = prisoner.prisonerNumber,
    prisonId = prisoner.prisonId,
    firstName = prisoner.firstName,
    lastName = prisoner.lastName,
    dateOfBirth = prisoner.dateOfBirth,
    cellLocation = prisoner.cellLocation,
    prisonName = prisoner.prisonName,
    category = inmateDetail.category,
    convictedStatus = prisoner.convictedStatus,
    incentiveLevel = prisoner.currentIncentive?.level?.description,
    alerts = prisonerAlerts,
    visitBalances = visitBalances,
    visits = visits,
    prisonerRestrictions = prisonerRestrictions,
  )

  override fun toString(): String = "PrisonerProfileDto(prisonerId='$prisonerId', prisonId=$prisonId, firstName='$firstName', lastName='$lastName', dateOfBirth=$dateOfBirth, cellLocation=$cellLocation, prisonName=$prisonName, category=$category, convictedStatus=$convictedStatus, incentiveLevel=$incentiveLevel, visitBalances=$visitBalances)"
}
