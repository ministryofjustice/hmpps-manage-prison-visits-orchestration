package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.orchestration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto

@JsonInclude(Include.NON_NULL)
@Schema(description = "Visit")
class VisitHistoryDetailsDto(

  @Schema(description = "The visit details", required = true)
  val eventsAudit: List<EventAuditOrchestrationDto> = listOf(),

  @Schema(description = "The visit details", required = true)
  @field:NotNull
  val visit: VisitDto,
)
