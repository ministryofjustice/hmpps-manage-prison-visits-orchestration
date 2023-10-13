package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.notifiers

const val PRISONER_NON_ASSOCIATION_DETAIL_CREATED_TYPE = "non-associations.created"
const val PRISONER_NON_ASSOCIATION_DETAIL_AMENDED_TYPE = "non-associations.amended"
const val PRISONER_NON_ASSOCIATION_DETAIL_CLOSED_TYPE = "non-associations.closed"
const val PRISONER_NON_ASSOCIATION_DETAIL_DELETED_TYPE = "non-associations.deleted"

enum class NonAssociationDomainEventType(val value: String) {
  NON_ASSOCIATION_CREATED(PRISONER_NON_ASSOCIATION_DETAIL_CREATED_TYPE),
  NON_ASSOCIATION_UPSERT(PRISONER_NON_ASSOCIATION_DETAIL_AMENDED_TYPE),
  NON_ASSOCIATION_CLOSED(PRISONER_NON_ASSOCIATION_DETAIL_CLOSED_TYPE),
  NON_ASSOCIATION_DELETED(PRISONER_NON_ASSOCIATION_DETAIL_DELETED_TYPE), ;

  companion object {
    fun getFromValue(value: String): NonAssociationDomainEventType? {
      return entries.firstOrNull { it.value.equals(value, true) }
    }
  }
}
