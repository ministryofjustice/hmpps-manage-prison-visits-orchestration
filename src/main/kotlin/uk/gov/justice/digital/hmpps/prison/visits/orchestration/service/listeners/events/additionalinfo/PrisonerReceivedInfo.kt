package uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.PrisonerReceivedReasonType

data class PrisonerReceivedInfo(
  @field:NotBlank
  @param:JsonProperty("nomsNumber")
  val prisonerNumber: String,

  @field:NotBlank
  @param:JsonProperty("prisonId")
  val prisonCode: String,

  @field:NotNull
  @param:JsonProperty("reason")
  val reason: PrisonerReceivedReasonType,
) : EventInfo
