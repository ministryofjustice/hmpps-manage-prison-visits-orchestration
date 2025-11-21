package uk.gov.justice.digital.hmpps.visits.orchestration.dto.orchestration

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visitor")
class OrchestrationVisitorDto(
  @param:Schema(description = "Person ID (nomis) of the visitor", example = "1234", required = true)
  @field:NotNull
  val nomisPersonId: Long,

  @param:Schema(description = "Visitor's first name", example = "James", required = false)
  val firstName: String? = null,

  @param:Schema(description = "Visitor's last name", example = "James", required = false)
  val lastName: String? = null,
)
