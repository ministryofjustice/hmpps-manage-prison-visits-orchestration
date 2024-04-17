package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A contact for a prisoner")
class VisitorBasicInfoDto(
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  val personId: Long,
  @Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
) {
  constructor(contact: ContactDto) : this(
    personId = contact.personId!!,
    firstName = contact.firstName,
    lastName = contact.lastName,
  )
}
