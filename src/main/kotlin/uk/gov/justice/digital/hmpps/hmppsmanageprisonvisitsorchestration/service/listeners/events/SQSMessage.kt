package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import com.fasterxml.jackson.annotation.JsonProperty

@Suppress("PropertyName")
data class SQSMessage(
  @JsonProperty("Type")
  val type: String,
  @JsonProperty("Message")
  val message: String,
  @JsonProperty("MessageId")
  val messageId: String? = null,
)
