package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.VisitorRestrictionDto
import java.time.LocalDate

@Schema(description = "A visitor for a prisoner")
data class VisitorInfoDto(
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  val visitorId: Long,
  @Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
  @Schema(description = "Date of birth", example = "2000-01-31", required = false)
  val dateOfBirth: LocalDate?,
  @Schema(description = "Relevant visitor restrictions that impact visits or empty list if none", required = true)
  val visitorRestrictions: Set<VisitorRestrictionDto>,
) {
  constructor(contact: PrisonerContactDto, visitorRestrictions: Set<VisitorRestrictionDto>) : this(
    visitorId = contact.personId!!,
    firstName = contact.firstName,
    lastName = contact.lastName,
    dateOfBirth = contact.dateOfBirth,
    visitorRestrictions = visitorRestrictions,
  )
}
