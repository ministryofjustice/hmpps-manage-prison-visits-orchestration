package uk.gov.justice.digital.hmpps.prison.visits.orchestration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PrisonVisitsOrchestration

fun main(args: Array<String>) {
  runApplication<PrisonVisitsOrchestration>(*args)
}
