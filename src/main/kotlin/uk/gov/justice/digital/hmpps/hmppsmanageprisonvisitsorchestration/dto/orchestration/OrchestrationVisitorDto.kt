package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visitor")
class OrchestrationVisitorDto(
  @Schema(description = "Person ID (nomis) of the visitor", example = "1234", required = true)
  @field:NotNull
  val nomisPersonId: Long,

  @Schema(description = "Visitor's first name", example = "James", required = false)
  var firstName: String? = null,

  @Schema(description = "Visitor's last name", example = "James", required = false)
  var lastName: String? = null,
)
