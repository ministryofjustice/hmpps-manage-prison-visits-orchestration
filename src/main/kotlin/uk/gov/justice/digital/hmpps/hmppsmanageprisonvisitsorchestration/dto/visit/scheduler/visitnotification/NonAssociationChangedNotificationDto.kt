package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers.NonAssociationDomainEventType

data class NonAssociationChangedNotificationDto(
  @NotBlank
  val prisonerNumber: String,
  @NotBlank
  val nonAssociationPrisonerNumber: String,
  @field:NotNull
  val type: NonAssociationDomainEventType,

) {

  constructor(info: NonAssociationChangedInfo, type: NonAssociationDomainEventType) : this(
    info.prisonerNumber,
    info.nonAssociationPrisonerNumber,
    type,
  )
}
