package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorRestrictionUpsertedInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class VisitorRestrictionUpsertedNotificationDto(
  @field:NotBlank
  val visitorId: String,

  @field:NotNull
  val validFromDate: LocalDate,

  @param:JsonInclude(Include.NON_NULL)
  val validToDate: LocalDate? = null,

  @field:NotBlank
  val restrictionType: String,

  @field:NotBlank
  val restrictionId: String,
) {

  constructor(info: VisitorRestrictionUpsertedInfo) : this(
    info.visitorId,
    LocalDate.parse(info.validFromDate, DateTimeFormatter.ISO_DATE),
    info.validToDate?.let { LocalDate.parse(info.validToDate, DateTimeFormatter.ISO_DATE) },
    info.restrictionType,
    info.restrictionId,
  )
}
