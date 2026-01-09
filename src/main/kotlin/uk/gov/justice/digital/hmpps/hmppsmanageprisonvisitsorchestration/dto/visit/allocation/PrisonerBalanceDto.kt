package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonerBalanceDto(
  @param:Schema(description = "nomsNumber of the prisoner", example = "AA123456", required = true)
  val prisonerId: String,

  @param:Schema(description = "The total of available and accumulated VO balance - any negative VO balance", example = "5", required = true)
  val voBalance: Int,

  @param:Schema(description = "The total of available PVO balance - any negative VO balance", example = "5", required = true)
  val pvoBalance: Int,

  @param:Schema(description = "First name of the prisoner", example = "John", required = true)
  val firstName: String,

  @param:Schema(description = "Last name of the prisoner", example = "Smith", required = true)
  val lastName: String,
)
