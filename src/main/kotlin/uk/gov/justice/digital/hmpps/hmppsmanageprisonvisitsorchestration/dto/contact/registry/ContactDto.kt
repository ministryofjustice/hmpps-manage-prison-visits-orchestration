package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
class ContactDto(
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791") val personId: Long? = null,
  @Schema(description = "First name", example = "John", required = true) val firstName: String,
  @Schema(description = "Middle name", example = "Mark", required = false) val middleName: String? = null,
  @Schema(description = "Last name", example = "Smith", required = true) val lastName: String,
  @Schema(description = "Date of birth", example = "1980-01-28", required = false) val dateOfBirth: LocalDate? = null,
  @Schema(description = "Code for relationship to Prisoner", example = "RO", required = true) val relationshipCode: String,
  @Schema(description = "Description of relationship to Prisoner", example = "Responsible Officer", required = false) val relationshipDescription: String? = null,
  @Schema(description = "Type of Contact", example = "O", required = true) val contactType: String,
  @Schema(description = "Description of Contact Type", example = "Official", required = false) val contactTypeDescription: String? = null,
  @Schema(description = "Approved Visitor Flag", required = true) val approvedVisitor: Boolean,
  @Schema(description = "Emergency Contact Flag", required = true) val emergencyContact: Boolean,
  @Schema(description = "Next of Kin Flag", required = true) val nextOfKin: Boolean,
  @Schema(description = "List of restrictions associated with the contact", required = false) val restrictions: List<RestrictionDto> = listOf(),
  @Schema(description = "List of addresses associated with the contact", required = false) var addresses: List<AddressDto> = listOf(),
  @Schema(description = "Additional Information", example = "This is a comment text", required = false) val commentText: String? = null,
)
