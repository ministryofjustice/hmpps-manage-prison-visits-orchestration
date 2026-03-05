package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

@Suppress("PropertyName")
data class SQSMessage(
  @field:NotBlank
  @param:JsonProperty("Message")
  val message: String,

  @param:JsonProperty("MessageId")
  val messageId: String? = null,

  @param:JsonProperty("MessageAttributes")
  val messageAttributes: MessageAttributes = MessageAttributes(),
)

data class MessageAttribute(
  @param:JsonProperty("Type")
  val type: String? = null,

  @param:JsonProperty("Value")
  val value: String? = null,
)

class MessageAttributes : HashMap<String, MessageAttribute>() {
  val eventType: String?
    get() = this["eventType"]?.value
}
