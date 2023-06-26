package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Contact")
class ContactDto(
  @Schema(description = "Contact Name", example = "John Smith", required = true)
  val name: String,
  @Schema(description = "Contact Phone Number", example = "01234 567890", required = true)
  val telephone: String?,
)
