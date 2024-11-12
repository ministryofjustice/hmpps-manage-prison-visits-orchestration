package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Contact")
class ContactDto(
  @Schema(description = "Contact Name", example = "John Smith", required = true)
  val name: String,
  @Schema(description = "Contact Phone Number", example = "01234 567890", required = false)
  val telephone: String? = null,
  @Schema(description = "Contact Email Address", example = "email@example.com", required = false)
  val email: String? = null,
)
