package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class NonAssociationChangedNotificationDto(
  val prisonerNumber: String,
  val nonAssociationPrisonerNumber: String,
  val validFromDate: LocalDate,
  val validToDate: LocalDate? = null,
) {

  constructor(info: NonAssociationChangedInfo) : this(
    info.prisonerNumber,
    info.nonAssociationPrisonerNumber,
    LocalDate.parse(info.validFromDate, DateTimeFormatter.ISO_DATE),
    info.validToDate?.let { LocalDate.parse(info.validToDate, DateTimeFormatter.ISO_DATE) },
  )
}
