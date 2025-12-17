package uk.gov.justice.digital.hmpps.orchestration.dto.prison.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Offender restrictions")
data class OffenderRestrictionsDto(
  @param:Schema(description = "Booking id for offender")
  val bookingId: Long? = null,

  @param:Schema(description = "Offender restrictions")
  val offenderRestrictions: List<OffenderRestrictionDto>? = null,
)
