package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation

import io.swagger.v3.oas.annotations.media.Schema

class VisitOrderHistoryDetailsDto(
  @param:Schema(description = "nomsNumber of the prisoner", example = "AA123456", required = true)
  val prisonerId: String,

  @param:Schema(required = true, description = "First Name", example = "Robert")
  val firstName: String,

  @param:Schema(required = true, description = "Last name", example = "Larsen")
  val lastName: String,

  @param:Schema(description = "Convicted Status", name = "convictedStatus", example = "Convicted", allowableValues = ["Convicted", "Remand"], required = false)
  val convictedStatus: String?,

  @param:Schema(description = "Incentive level", example = "Standard", required = false)
  val incentiveLevel: String?,

  @param:Schema(description = "Category description (from list of assessments)", example = "Category C")
  val category: String?,

  @param:Schema(description = "List of Visit Order History")
  var visitOrderHistory: List<VisitOrderHistoryDto>,
)
