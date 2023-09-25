package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class NonAssociationChangedNotificationDto(
  @NotBlank
  val prisonerNumber: String,
  @NotBlank
  val nonAssociationPrisonerNumber: String,
  @NotNull
  val validFromDate: LocalDate,
  @JsonInclude(Include.NON_NULL)
  val validToDate: LocalDate? = null,
) {

  constructor(info: NonAssociationChangedInfo) : this(
    info.prisonerNumber,
    info.nonAssociationPrisonerNumber,
    LocalDate.parse(info.validFromDate, DateTimeFormatter.ISO_DATE),
    info.validToDate?.let { LocalDate.parse(info.validToDate, DateTimeFormatter.ISO_DATE) },
  )
}
