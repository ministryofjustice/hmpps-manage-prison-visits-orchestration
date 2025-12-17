package uk.gov.justice.digital.hmpps.prison.visits.orchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.enums.ApplicationValidationErrorCodes

class ApplicationValidationException(val errorCodes: List<ApplicationValidationErrorCodes>) : ValidationException()
