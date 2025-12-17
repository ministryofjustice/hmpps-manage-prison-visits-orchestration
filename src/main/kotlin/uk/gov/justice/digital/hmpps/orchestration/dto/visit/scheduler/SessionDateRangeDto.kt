package uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

class SessionDateRangeDto(
  @param:Schema(description = "The start of the Validity period for the session template", example = "2019-11-02", required = true)
  val validFromDate: LocalDate,

  @param:Schema(description = "The end of the Validity period for the session template", example = "2019-12-02", required = false)
  val validToDate: LocalDate? = null,
)
