package uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Contact")
open class ContactDto(
  @param:Schema(description = "Contact Name", example = "John Smith", required = true)
  open val name: String,
  @param:Schema(description = "Contact Phone Number", example = "01234 567890", required = false)
  open val telephone: String? = null,
  @param:Schema(description = "Contact Email Address", example = "email@example.com", required = false)
  open val email: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ContactDto

    if (name != other.name) return false
    if (telephone != other.telephone) return false
    if (email != other.email) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + (telephone?.hashCode() ?: 0)
    result = 31 * result + (email?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String = "ContactDto(name='$name', telephone=$telephone, email=$email)"
}
