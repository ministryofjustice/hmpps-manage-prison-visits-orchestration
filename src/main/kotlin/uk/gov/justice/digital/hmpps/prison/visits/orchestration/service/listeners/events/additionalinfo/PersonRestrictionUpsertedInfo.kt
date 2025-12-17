package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PersonRestrictionUpsertedInfo(
  @field:NotBlank
  @param:JsonProperty("nomsNumber")
  val prisonerNumber: String,

  @field:NotBlank
  @param:JsonProperty("personId")
  val visitorId: String,

  @field:NotBlank
  @param:JsonProperty("effectiveDate")
  val validFromDate: String,

  @param:JsonInclude(Include.NON_NULL)
  @param:JsonProperty("expiryDate")
  val validToDate: String? = null,

  @field:NotBlank
  val restrictionType: String,

  @field:NotBlank
  @param:JsonProperty("offenderPersonRestrictionId")
  val restrictionId: String,
) : EventInfo
