package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.AddressDto
import java.time.LocalDate

@Schema(description = "Visit Pass Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitPassVisitorDto(
  @param:Schema(description = "Person ID (nomis) of the visitor", example = "1234", required = true)
  @field:NotNull
  val nomisPersonId: Long,

  @param:Schema(description = "Visitor's first name", example = "John", required = true)
  var firstName: String,

  @param:Schema(description = "Visitor's last name", example = "Smith", required = true)
  var lastName: String,

  @param:Schema(description = "Visitor's date of birth", example = "2000-01-01", required = false)
  var dob: LocalDate?,

  @param:Schema(description = "Address associated with the contact", required = false)
  var address: AddressDto?,
)
