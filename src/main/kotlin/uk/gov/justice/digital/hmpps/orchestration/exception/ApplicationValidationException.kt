package uk.gov.justice.digital.hmpps.orchestration.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.enums.ApplicationValidationErrorCodes

class ApplicationValidationException(val errorCodes: List<ApplicationValidationErrorCodes>) : ValidationException()
