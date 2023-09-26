package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class NonAssociationChangedInfo(
  @NotBlank
  @JsonProperty("nomsNumber")
  val prisonerNumber: String,
  @NotBlank
  @JsonProperty("nonAssociationNomsNumber")
  val nonAssociationPrisonerNumber: String,
  @NotBlank
  @JsonProperty("effectiveDate")
  val validFromDate: String,
  @JsonInclude(Include.NON_NULL)
  @JsonProperty("expiryDate")
  val validToDate: String? = null,
)
