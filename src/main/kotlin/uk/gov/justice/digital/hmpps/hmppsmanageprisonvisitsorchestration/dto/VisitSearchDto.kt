package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import java.time.LocalDateTime

data class VisitSearchDto(
  val prisonCode: String?,
  val prisonerId: String?,
  val visitorId: String? = null,
  val startDateTime: LocalDateTime? = null,
  val endDateTime: LocalDateTime? = null,
  val visitStatusList: List<String>,
  val page: Int = 0,
  val size: Int = 20
)
