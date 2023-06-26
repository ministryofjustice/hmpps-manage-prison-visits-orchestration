package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Telephone Details")
class TelephoneDto(
  @Schema(description = "Telephone number", example = "0114 2345678", required = true) val number: String,
  @Schema(description = "Telephone type", example = "TEL", required = true) val type: String,
  @Schema(description = "Telephone extension number", example = "123", required = false) val ext: String? = null,
)
