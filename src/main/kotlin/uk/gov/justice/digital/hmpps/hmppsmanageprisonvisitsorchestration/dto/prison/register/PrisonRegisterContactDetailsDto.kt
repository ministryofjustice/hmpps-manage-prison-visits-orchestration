package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison Contact Details such as email address, phone number and website")
data class PrisonRegisterContactDetailsDto(
  @Schema(description = "Contact email address of prison", example = "example@example.com", required = false)
  val emailAddress: String? = null,
  @Schema(description = "Contact number of prison", required = false)
  val phoneNumber: String? = null,
  @Schema(description = "Website of prison", required = false)
  val website: String? = null,
)
