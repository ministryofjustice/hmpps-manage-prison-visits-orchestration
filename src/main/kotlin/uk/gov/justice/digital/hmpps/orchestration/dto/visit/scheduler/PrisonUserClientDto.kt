package uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.enums.UserType

@Schema(description = "Prison user client dto")
class PrisonUserClientDto(

  @param:Schema(description = "User type", example = "STAFF", required = true)
  @field:NotNull
  val userType: UserType,

  @param:Schema(description = "is prison user client active", example = "true", required = true)
  @field:NotNull
  var active: Boolean,
)
