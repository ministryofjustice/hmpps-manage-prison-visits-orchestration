package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Prison Information")
data class PrisonNameDto(
  @param:Schema(description = "Prison ID", example = "MDI", required = true)
  val prisonId: String,
  @param:Schema(description = "Name of the prison", example = "Moorland HMP", required = true)
  val prisonName: String,

  @param:JsonInclude(JsonInclude.Include.NON_NULL)
  @param:Schema(description = "Name of the prison in Welsh", example = "Carchar Brynbuga", required = false)
  val prisonNameInWelsh: String? = null,
)
