package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.visit.scheduler.visitnotification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.events.additionalinfo.NonAssociationChangedInfo
import uk.gov.justice.digital.hmpps.prison.visits.orchestration.service.listeners.notifiers.NonAssociationDomainEventType

data class NonAssociationChangedNotificationDto(
  @field:NotBlank
  val prisonerNumber: String,
  @field:NotBlank
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
