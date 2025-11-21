package uk.gov.justice.digital.hmpps.visits.orchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visitor")
data class VisitorDto(
  @param:Schema(description = "Person ID (nomis) of the visitor", example = "1234", required = true)
  @field:NotNull
  val nomisPersonId: Long,

  @param:Schema(description = "true if visitor is the contact for the visit otherwise false", example = "true", required = false)
  val visitContact: Boolean?,
)
