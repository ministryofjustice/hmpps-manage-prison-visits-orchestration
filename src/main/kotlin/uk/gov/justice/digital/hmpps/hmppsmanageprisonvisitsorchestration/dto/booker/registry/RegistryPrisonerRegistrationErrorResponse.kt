package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums.RegistryPrisonerRegistrationErrorCodes

internal data class RegistryPrisonerRegistrationErrorResponse(
  val validationErrors: List<RegistryPrisonerRegistrationErrorCodes>,
)
