package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema

data class SessionTemplateDto(
  @param:Schema(description = "Reference", example = "v9d.7ed.7u", required = true)
  val reference: String,
)
