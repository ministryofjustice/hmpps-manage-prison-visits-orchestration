package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.booker.registry.BookerHistoryAuditDto
import java.time.Duration

@Component
class BookerAuditHistoryClient(
  private val visitSchedulerClient: VisitSchedulerClient,
  private val bookerRegistryClient: PrisonVisitBookerRegistryClient,
  @Value("\${prison-visit-booker-registry.api.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getBookerAuditHistory(
    bookerReference: String,
  ): List<BookerHistoryAuditDto> {
    LOG.trace("getBookerAuditHistory for booker reference - {}", bookerReference)
    val visitAuditEntries = visitSchedulerClient.getBookerHistoryAsMono(bookerReference)
    val bookerRegistryAuditEntries = bookerRegistryClient.getBookerAuditHistoryAsMono(bookerReference)

    val combinedAuditHistory = Mono.zip(visitAuditEntries, bookerRegistryAuditEntries).map { bookerHistoryMonos ->
      val visitAuditHistory = bookerHistoryMonos.t1.map { BookerHistoryAuditDto(it) }
      val bookerRegistryAuditHistory = bookerHistoryMonos.t2.map { BookerHistoryAuditDto(it) }
      visitAuditHistory + bookerRegistryAuditHistory
    }.block(apiTimeout)

    return combinedAuditHistory?.sortedBy { it.createdTimestamp } ?: emptyList()
  }
}
