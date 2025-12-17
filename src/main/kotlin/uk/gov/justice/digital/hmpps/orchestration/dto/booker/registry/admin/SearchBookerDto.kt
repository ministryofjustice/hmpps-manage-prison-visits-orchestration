package uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.admin

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Find a booker via search criteria")
data class SearchBookerDto(
  @param:Schema(name = "email", description = "auth email", required = true)
  @field:NotBlank
  val email: String,
)
