package uk.gov.justice.digital.hmpps.visitscheduler.dto.sessions

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

class SessionDateRangeDto(
  @Schema(description = "The start of the Validity period for the session template", example = "2019-11-02", required = true)
  val validFromDate: LocalDate,

  @Schema(description = "The end of the Validity period for the session template", example = "2019-12-02", required = false)
  val validToDate: LocalDate? = null,
)
