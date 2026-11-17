package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ValidationErrorCodes

enum class BookerPrisonerRegistrationErrorCodes : ValidationErrorCodes {
  PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER,
  FAILED_REGISTRATION,
}
