package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.enums

enum class BookerAuditType(val telemetryEventName: String) {
  BOOKER_CREATED("booker_created"),
  PRISONER_REGISTERED("prisoner_registered"),
  VISITOR_ADDED_TO_PRISONER("visitor_added"),
  VISITOR_REQUEST_APPROVED_FOR_PRISONER("visitor_request_approved"),
  VISITOR_REQUEST_AUTO_APPROVED_FOR_PRISONER("visitor_request_auto_approved"),
  VISITOR_REQUEST_REJECTED_FOR_PRISONER("visitor_request_rejected"),
  VISITOR_REQUEST_WITHDRAWN_FOR_PRISONER("visitor_request_withdrawn"),
  ACTIVATED_PRISONER("prisoner_activated"),
  DEACTIVATED_PRISONER("prisoner_deactivated"),
  ACTIVATED_VISITOR("visitor_activated"),
  DEACTIVATED_VISITOR("visitor_deactivated"),
  UNLINK_VISITOR("visitor_unlinked"),
  CLEAR_BOOKER_DETAILS("booker_details_cleared"),
  UPDATE_BOOKER_EMAIL("booker_email_updated"),
  REGISTER_PRISONER_SEARCH("prisoner_search"),
  UPDATE_REGISTERED_PRISONER_PRISON("prisoner_update_prison"),
  VISITOR_REQUEST_SUBMITTED("visitor_request_submitted"),
}
