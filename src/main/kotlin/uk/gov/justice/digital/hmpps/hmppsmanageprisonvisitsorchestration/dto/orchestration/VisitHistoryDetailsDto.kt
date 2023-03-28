package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import java.time.LocalDateTime

@JsonInclude(Include.NON_NULL)
@Schema(description = "Visit")
class VisitHistoryDetailsDto(
  @Schema(description = "Created By - user details  for the user who created the visit, NOT_KNOWN is used for historical cases", example = "AB12345A", required = true)
  @field:NotNull
  val createdBy: String,
  @Schema(description = "Updated By - user details for the user who last updated the visit", example = "AB12345A", required = false)
  val updatedBy: String? = null,
  @Schema(description = "Cancelled By - user details for the user who cancelled the visit", example = "AB12345A", required = false)
  val cancelledBy: String? = null,

  @Schema(description = "The visit created date and time", example = "2018-12-01T13:45:00", required = true)
  @field:NotNull
  val createdDateAndTime: LocalDateTime,

  @Schema(description = "The visit updated date and time", example = "2018-12-01T13:45:00", required = false)
  val updatedDateAndTime: LocalDateTime? = null,

  @Schema(description = "The visit cancelled date and time", example = "2018-12-01T13:45:00", required = false)
  val cancelledDateAndTime: LocalDateTime ? = null,

  @Schema(description = "The visit details", required = true)
  @field:NotNull
  val visit: VisitDto,
)
