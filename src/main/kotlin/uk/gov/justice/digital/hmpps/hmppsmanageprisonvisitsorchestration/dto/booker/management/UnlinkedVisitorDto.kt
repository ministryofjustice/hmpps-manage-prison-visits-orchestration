package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.management

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.PrisonerContactDto
import java.time.LocalDate

@Schema(description = "Visitor details")
data class UnlinkedVisitorDto(
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  val visitorId: Long,

  @Schema(description = "First name", example = "John", required = true)
  val firstName: String,

  @Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,

  @Schema(description = "Date of birth", example = "2000-01-31", required = false)
  val dateOfBirth: LocalDate?,
) {
  constructor(contact: PrisonerContactDto) : this(
    visitorId = contact.personId!!,
    firstName = contact.firstName,
    lastName = contact.lastName,
    dateOfBirth = contact.dateOfBirth,
  )

  @Schema(description = "Date when visitor was last approved for a visit (approved / auto-approved)", example = "2025-09-12", required = false)
  var lastApprovedForVisitDate: LocalDate? = null
}
