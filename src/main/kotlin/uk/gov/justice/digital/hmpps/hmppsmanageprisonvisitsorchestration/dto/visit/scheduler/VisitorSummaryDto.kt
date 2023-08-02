package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Full Visitor details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitorSummaryDto(
  @Schema(description = "Person ID (nomis) of the visitor", example = "1234", required = true)
  @field:NotNull
  val nomisPersonId: Long,

  @Schema(description = "Visitor's first name", example = "John", required = false)
  var firstName: String? = null,

  @Schema(description = "Visitor's last name", example = "Smith", required = false)
  var lastName: String? = null,
) {
  constructor(
    visitorDto: VisitorDto,
  ) :
    this(
      nomisPersonId = visitorDto.nomisPersonId,
    )
}
