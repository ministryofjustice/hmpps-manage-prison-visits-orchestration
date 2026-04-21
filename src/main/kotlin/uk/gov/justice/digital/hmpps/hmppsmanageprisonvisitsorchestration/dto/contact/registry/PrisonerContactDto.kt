package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
data class PrisonerContactDto(
  @param:Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791")
  val personId: Long? = null,

  @param:Schema(description = "First name", example = "John", required = true)
  val firstName: String,

  @param:Schema(description = "Middle name", example = "Mark", required = false)
  val middleName: String? = null,

  @param:Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,

  @param:Schema(description = "Date of birth", example = "1980-01-28", required = false)
  val dateOfBirth: LocalDate? = null,

  @param:Schema(description = "Code for relationship to Prisoner", example = "RO", required = true)
  val relationshipCode: String,

  @param:Schema(description = "Description of relationship to Prisoner", example = "Responsible Officer", required = false)
  val relationshipDescription: String? = null,

  @param:Schema(description = "Type of Contact", example = "O", required = true)
  val contactType: String,

  @param:Schema(description = "Description of Contact Type", example = "Official", required = false)
  val contactTypeDescription: String? = null,

  @param:Schema(description = "Approved Visitor Flag", required = true)
  val approvedVisitor: Boolean,

  @param:Schema(description = "Emergency Contact Flag", required = true)
  val emergencyContact: Boolean,

  @param:Schema(description = "Next of Kin Flag", required = true)
  val nextOfKin: Boolean,

  @param:Schema(description = "List of restrictions associated with the contact", required = false)
  val restrictions: List<RestrictionDto> = listOf(),

  @param:Schema(description = "Address associated with the contact", required = false)
  var address: AddressDto? = null,

  @param:Schema(description = "Additional Information", example = "This is a comment text", required = false)
  val commentText: String? = null,
)
