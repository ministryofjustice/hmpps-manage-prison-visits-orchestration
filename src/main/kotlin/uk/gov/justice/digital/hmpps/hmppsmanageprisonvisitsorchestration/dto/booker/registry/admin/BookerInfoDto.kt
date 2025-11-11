package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.admin

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.PermittedPrisonerForBookerDto
import java.time.LocalDateTime

@Schema(description = "Details of a found booker via booker search")
data class BookerInfoDto(
  @param:Schema(name = "reference", description = "This is the booker reference, unique per booker", required = true)
  @field:NotBlank
  val reference: String,

  @param:Schema(name = "email", description = "email registered to booker", required = true)
  @field:NotBlank
  val email: String,

  @param:Schema(name = "createdTimestamp", description = "The time the booker account was created", required = true)
  @field:NotBlank
  val createdTimestamp: LocalDateTime,

  @JsonProperty("permittedPrisoners")
  @param:Schema(description = "Permitted prisoners list", required = true)
  @field:Valid
  val permittedPrisoners: List<PermittedPrisonerForBookerDto>,
)
