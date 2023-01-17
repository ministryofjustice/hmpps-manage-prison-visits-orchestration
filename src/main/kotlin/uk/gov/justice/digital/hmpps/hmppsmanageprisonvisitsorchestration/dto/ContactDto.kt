package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Contact")
class ContactDto(
  @Schema(description = "Contact Name", example = "John Smith", required = true)
  val name: String,
  @Schema(description = "Contact Phone Number", example = "01234 567890", required = true)
  val telephone: String?,
)
