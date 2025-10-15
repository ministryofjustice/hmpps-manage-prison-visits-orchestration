package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@Schema(description = "Detailed information of a booker")
data class BookerDetailedInfoDto(
  @Schema(name = "reference", description = "This is the booker reference, unique per booker", required = true)
  @field:NotBlank
  val reference: String,

  @Schema(name = "email", description = "email registered to booker", required = true)
  @field:NotBlank
  val email: String,

  @Schema(description = "Permitted prisoners list", required = true)
  @field:Valid
  val permittedPrisoners: List<BookerPrisonerDetailedInfoDto>,
)
