package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

enum class NotificationEventType {
  NON_ASSOCIATION_EVENT,
  PRISONER_RELEASED_EVENT,
  PRISONER_RESTRICTION_CHANGE_EVENT,
  PRISON_VISITS_BLOCKED_FOR_DATE,
  PRISONER_RECEIVED_EVENT,
  PRISONER_ALERTS_UPDATED_EVENT,
  PERSON_RESTRICTION_UPSERTED_EVENT,
}
