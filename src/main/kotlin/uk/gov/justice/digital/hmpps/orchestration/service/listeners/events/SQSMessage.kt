package uk.gov.justice.digital.hmpps.orchestration.service.listeners.events

import com.fasterxml.jackson.annotation.JsonProperty

@Suppress("PropertyName")
data class SQSMessage(
  @param:JsonProperty("Type")
  val type: String,
  @param:JsonProperty("Message")
  val message: String,
  @param:JsonProperty("MessageId")
  val messageId: String? = null,
)
