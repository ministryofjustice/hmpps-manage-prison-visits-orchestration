package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class VisitSearchRequestFilter(
  @Parameter(
    description = "Filter results by prison id/code",
    example = "MDI",
  )
  val prisonCode: String? = null,

  @Parameter(
    description = "Filter results by prisoner id",
    example = "A12345DC",
  )
  val prisonerId: String?,

  @Parameter(
    description = "Filter results by visitor (contact id)",
    example = "12322",
  )
  val visitorId: String? = null,

  @Parameter(
    description = "Filter results by visits that start on or after the given timestamp",
    example = "2021-11-03T09:00:00",
  )
  val startDateTime: LocalDateTime? = null,

  @Parameter(
    description = "Filter results by visits that start on or before the given timestamp",
    example = "2021-11-03T09:00:00",
  )
  val endDateTime: LocalDateTime? = null,

  @Parameter(
    description = "Filter results by visit status",
    example = "BOOKED",
  )
  @NotNull
  @NotEmpty
  val visitStatusList: List<String>,

  @Parameter(
    description = "Pagination page number, starting at zero",
    example = "0",
  )
  val page: Int = 0,

  @Parameter(
    description = "Pagination size per page",
    example = "50",
  )
  val size: Int = 20,
)
