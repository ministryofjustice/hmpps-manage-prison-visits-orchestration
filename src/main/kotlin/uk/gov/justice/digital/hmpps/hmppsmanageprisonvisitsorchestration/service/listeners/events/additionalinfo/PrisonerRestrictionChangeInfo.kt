package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PrisonerRestrictionChangeInfo(
  @field:NotBlank
  @param:JsonProperty("nomsNumber")
  val prisonerNumber: String,
  @field:NotBlank
  @param:JsonProperty("effectiveDate")
  val validFromDate: String,
  @param:JsonInclude(Include.NON_NULL)
  @param:JsonProperty("expiryDate")
  val validToDate: String? = null,
) : EventInfo
