package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.admin

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@Schema(description = "Detailed information of a booker")
data class BookerDetailedInfoDto(
  @param:Schema(name = "reference", description = "This is the booker reference, unique per booker", required = true)
  @field:NotBlank
  val reference: String,

  @param:Schema(name = "email", description = "email registered to booker", required = true)
  @field:NotBlank
  val email: String,

  @param:Schema(description = "Permitted prisoners list", required = true)
  @field:Valid
  val permittedPrisoners: List<BookerPrisonerDetailedInfoDto>,
)
