package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.management.SocialContactsDto
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.booker.registry.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.prisoner.search.PrisonerDto
import java.time.LocalDate

data class SingleVisitorRequestForReviewDto(
  @param:Schema(description = "Visitor Request reference", example = "abc-def-ghi", required = true)
  @field:NotBlank
  val reference: String,

  @param:Schema(description = "Booker reference", example = "wwe-egg-wwf", required = true)
  @field:NotBlank
  val bookerReference: String,

  @param:Schema(description = "Booker email", example = "test@test.com", required = true)
  @field:NotBlank
  val bookerEmail: String,

  @param:Schema(description = "Prisoner ID for whom visitor was requested", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @param:Schema(description = "Prisoner first name", example = "John", required = true)
  @field:NotBlank
  val prisonerFirstName: String,

  @param:Schema(description = "Prisoner last name", example = "Smith", required = true)
  @field:NotBlank
  val prisonerLastName: String,

  @param:Schema(description = "First Name, as entered on visitor request", example = "John", required = true)
  @field:NotBlank
  val firstName: String,

  @param:Schema(description = "Last Name, as entered on visitor request", example = "Smith", required = true)
  @field:NotBlank
  val lastName: String,

  @param:Schema(description = "Date of birth, as entered on visitor request", example = "2000-01-01", required = true)
  val dateOfBirth: LocalDate,

  @param:Schema(description = "Date request was submitted", example = "2025-10-28", required = true)
  val requestedOn: LocalDate,

  @param:Schema(description = "The current status of the request", example = "REQUESTED", required = true)
  val status: VisitorRequestsStatus,

  @param:Schema(description = "Date request was submitted", example = "2025-10-28", required = true)
  val socialContacts: List<SocialContactsDto>,
) {
  constructor(request: PrisonVisitorRequestDto, prisonerInfo: PrisonerDto, contacts: List<SocialContactsDto>) : this(
    reference = request.reference,
    bookerReference = request.bookerReference,
    bookerEmail = request.bookerEmail,
    prisonerId = request.prisonerId,
    prisonerFirstName = prisonerInfo.firstName,
    prisonerLastName = prisonerInfo.lastName,
    firstName = request.firstName,
    lastName = request.lastName,
    dateOfBirth = request.dateOfBirth,
    requestedOn = request.requestedOn,
    socialContacts = contacts,
    status = request.status,
  )
}
