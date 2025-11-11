package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.filter

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

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
    description = "Filter results by visits that start on or after the given date",
    example = "2021-11-03",
  )
  val visitStartDate: LocalDate? = null,

  @Parameter(
    description = "Filter results by visits that start on or before the given date",
    example = "2021-11-03",
  )
  val visitEndDate: LocalDate? = null,

  @Parameter(
    description = "Filter results by visit status",
    example = "BOOKED",
  )
  @field:NotNull
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
