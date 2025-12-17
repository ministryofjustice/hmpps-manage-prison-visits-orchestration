package uk.gov.justice.digital.hmpps.orchestration.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema

data class AttributeSearchPrisonerDto(
  @param:Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String,

  @param:Schema(required = true, description = "First Name", example = "Robert")
  val firstName: String,

  @param:Schema(required = true, description = "Last name", example = "Larsen")
  val lastName: String,
)
