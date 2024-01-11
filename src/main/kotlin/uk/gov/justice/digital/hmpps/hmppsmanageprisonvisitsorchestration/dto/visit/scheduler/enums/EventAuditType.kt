package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums

@Suppress("unused")
enum class EventAuditType {
  RESERVED_VISIT, CHANGING_VISIT, MIGRATED_VISIT, BOOKED_VISIT, UPDATED_VISIT, CANCELLED_VISIT,
  NON_ASSOCIATION_EVENT, PRISONER_RELEASED_EVENT, PRISONER_RESTRICTION_CHANGE_EVENT, PRISON_VISITS_BLOCKED_FOR_DATE
}
