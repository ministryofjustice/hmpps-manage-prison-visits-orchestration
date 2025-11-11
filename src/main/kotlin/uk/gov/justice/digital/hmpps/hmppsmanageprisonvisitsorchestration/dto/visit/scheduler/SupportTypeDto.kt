package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Support Type")
class SupportTypeDto(
  @param:Schema(description = "Support type name", example = "MASK_EXEMPT", required = true)
  val type: String,
  @param:Schema(description = "Support description", example = "Face covering exemption", required = true)
  val description: String,
)
