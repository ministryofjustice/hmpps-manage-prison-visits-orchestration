package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.PrisonService

@Component
class PublicBookerValidationUtil {
  companion object {
    const val PRISONER_VALIDATION_ERROR_MSG = "prisoner validation for prisoner number - {0} failed with error - {1}"
    const val PRISON_VALIDATION_ERROR_MSG =
      "prison validation for prison code - {0} for prisoner number - {1} failed with error - {2}"
  }

  fun validatePrison(prison: VisitSchedulerPrisonDto, prisonService: PrisonService): String? {
    var errorMessage: String? = null

    if (!prisonService.isActive(prison)) {
      errorMessage = "Prison with code - ${prison.code}, is not active on visit-scheduler"
    } else {
      if (!prisonService.isActive(prison, UserType.PUBLIC)) {
        errorMessage = "Prison with code - ${prison.code}, is not active for public users on visit-scheduler"
      }
    }

    return errorMessage
  }

  fun validatePrisoner(prisonerNumber: String, offenderSearchPrisoner: PrisonerDto): String? {
    var errorMessage: String? = null

    if (offenderSearchPrisoner.prisonId == null) {
      // if the offender was found but without a prison code throw an exception
      errorMessage = "Prisoner - $prisonerNumber on prisoner search does not have a valid prison"
    }

    return errorMessage
  }
}
