package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitStatus
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import java.time.LocalDateTime

@Schema(description = "Visit Summary")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitSummaryDto(
  @Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  val prisonerId: String,
  @JsonProperty("prisonId")
  @JsonAlias("prisonCode")
  @Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
  @Schema(description = "Prison name", example = "MDI Prison", required = false)
  var prisonName: String? = null,
  @Schema(description = "Visit Type", example = "SOCIAL", required = true)
  val visitType: VisitType,
  @Schema(description = "Visit Status", example = "BOOKED", required = true)
  val visitStatus: VisitStatus,
  @Schema(description = "Visit Sub Status", example = "APPROVED", required = true)
  val visitSubStatus: VisitSubStatus,
  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val startTimestamp: LocalDateTime,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  @field:NotBlank
  val endTimestamp: LocalDateTime,
  @Schema(description = "List of visitors associated with the visit", required = false)
  val visitors: List<VisitorSummaryDto>? = listOf(),
) {
  constructor(
    visitDto: VisitDto,
  ) :
    this(
      reference = visitDto.reference,
      prisonerId = visitDto.prisonerId,
      prisonCode = visitDto.prisonCode,
      visitType = visitDto.visitType,
      visitStatus = visitDto.visitStatus,
      visitSubStatus = visitDto.visitSubStatus,
      visitRestriction = visitDto.visitRestriction,
      startTimestamp = visitDto.startTimestamp,
      endTimestamp = visitDto.endTimestamp,
      visitors = visitDto.visitors?.map { VisitorSummaryDto(it) },
    )
}
