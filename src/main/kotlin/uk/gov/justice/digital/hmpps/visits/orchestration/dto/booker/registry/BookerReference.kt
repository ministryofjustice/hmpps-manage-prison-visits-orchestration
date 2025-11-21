package uk.gov.justice.digital.hmpps.visits.orchestration.dto.booker.registry

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Booker reference Object, to be used with all other api call for booker information")
data class BookerReference(
  @param:JsonProperty("value")
  @param:Schema(name = "value", description = "This value is the booker reference and should be used to acquire booker information", required = true)
  @field:NotBlank
  val value: String,
)
