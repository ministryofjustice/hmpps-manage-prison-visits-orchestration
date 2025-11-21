package uk.gov.justice.digital.hmpps.visits.orchestration.task

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled task to evict cache to ensure cache is refreshed at regular intervals.
 */
@Component
class ScheduledCacheEvictTask {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Scheduled(cron = "\${cache.evict.bank-holidays.cron:0 0 0 */7 * ?}")
  @CacheEvict(value = ["bank-holidays"], allEntries = true)
  fun evictBankHolidaysCache() {
    LOG.debug("Evicting bank holidays cache.")
  }
}
