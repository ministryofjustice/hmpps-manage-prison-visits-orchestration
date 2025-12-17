package uk.gov.justice.digital.hmpps.orchestration.dto.prison.register

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison Information")
data class PrisonRegisterPrisonDto(
  @param:Schema(description = "Prison ID", example = "MDI", required = true)
  val prisonId: String,
  @param:Schema(description = "Name of the prison", example = "Moorland HMP", required = true)
  val prisonName: String,
)
