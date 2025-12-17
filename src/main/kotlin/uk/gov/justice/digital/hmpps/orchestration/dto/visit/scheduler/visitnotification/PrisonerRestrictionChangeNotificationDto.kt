package uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.visitnotification

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.orchestration.service.listeners.events.additionalinfo.PrisonerRestrictionChangeInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PrisonerRestrictionChangeNotificationDto(
  @field:NotBlank
  val prisonerNumber: String,
  @field:NotNull
  val validFromDate: LocalDate,
  @param:JsonInclude(Include.NON_NULL)
  val validToDate: LocalDate? = null,
) {

  constructor(info: PrisonerRestrictionChangeInfo) : this(
    info.prisonerNumber,
    LocalDate.parse(info.validFromDate, DateTimeFormatter.ISO_DATE),
    info.validToDate?.let { LocalDate.parse(info.validToDate, DateTimeFormatter.ISO_DATE) },
  )
}
