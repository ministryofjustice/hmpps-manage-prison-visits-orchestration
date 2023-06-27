package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Full Visitor details")
@JsonInclude(JsonInclude.Include.NON_NULL)
class FullVisitorDetailsDto(
  nomisPersonId: Long,
  visitContact: Boolean?,
  @Schema(description = "Visitor's first name", example = "John", required = false)
  var firstName: String? = null,

  @Schema(description = "Visitor's last name", example = "Smith", required = false)
  var lastName: String? = null,
) : VisitorDto(
  nomisPersonId = nomisPersonId,
  visitContact = visitContact,
) {
  constructor(
    visitorDto: VisitorDto,
  ) :
    this(
      nomisPersonId = visitorDto.nomisPersonId,
      visitContact = visitorDto.visitContact,
    )
}
