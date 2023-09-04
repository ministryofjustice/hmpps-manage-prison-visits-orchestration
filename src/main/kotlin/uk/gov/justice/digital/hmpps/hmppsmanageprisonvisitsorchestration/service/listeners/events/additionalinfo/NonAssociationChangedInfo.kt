package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty

data class NonAssociationChangedInfo(
  @JsonProperty("nomsNumber")
  val prisonerNumber: String,
  @JsonProperty("nonAssociationNomsNumber")
  val nonAssociationPrisonerNumber: String,
  @JsonProperty("effectiveDate")
  val validFromDate: String,
  @JsonInclude(Include.NON_NULL)
  @JsonProperty("expiryDate")
  val validToDate: String? = null,
)
