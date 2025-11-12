package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.register

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison Contact Details such as email address, phone number and web address")
data class PrisonRegisterContactDetailsDto(
  @param:Schema(description = "Contact email address of prison", example = "example@example.com", required = false)
  val emailAddress: String? = null,
  @param:Schema(description = "Contact number of prison", required = false)
  val phoneNumber: String? = null,
  @param:Schema(description = "Web address of prison", required = false)
  val webAddress: String? = null,
)
