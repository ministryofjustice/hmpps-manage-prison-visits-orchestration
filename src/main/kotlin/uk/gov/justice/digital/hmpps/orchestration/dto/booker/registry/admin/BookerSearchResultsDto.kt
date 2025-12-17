package uk.gov.justice.digital.hmpps.orchestration.dto.booker.registry.admin

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "Details of a found booker via booker search")
data class BookerSearchResultsDto(
  @param:Schema(name = "reference", description = "This is the booker reference, unique per booker", required = true)
  @field:NotBlank
  val reference: String,

  @param:Schema(name = "email", description = "email registered to booker", required = true)
  @field:NotBlank
  val email: String,

  @param:Schema(name = "createdTimestamp", description = "The time the booker account was created", required = true)
  @field:NotBlank
  val createdTimestamp: LocalDateTime,
)
