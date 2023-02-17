package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Balances of visit orders and privilege visit orders")
data class VisitBalancesDto(
  @Schema(required = true, description = "Balance of visit orders remaining")
  var remainingVo: Int? = null,

  @Schema(required = true, description = "Balance of privilege visit orders remaining")
  var remainingPvo: Int? = null,

  @Schema(description = "Date of last IEP adjustment for Visit orders")
  var latestIepAdjustDate: LocalDate? = null,

  @Schema(description = "Date of last IEP adjustment for Privilege Visit orders")
  var latestPrivIepAdjustDate: LocalDate? = null
)
