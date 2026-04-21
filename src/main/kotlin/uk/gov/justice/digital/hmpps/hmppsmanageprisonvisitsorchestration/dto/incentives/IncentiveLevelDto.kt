package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.incentives

import io.swagger.v3.oas.annotations.media.Schema

class IncentiveLevelDto(
  @param:Schema(description = "Incentive level code", example = "STD", required = true)
  val code: String,

  @param:Schema(required = true, description = "Incentive level name", example = "Standard")
  val name: String,
)
