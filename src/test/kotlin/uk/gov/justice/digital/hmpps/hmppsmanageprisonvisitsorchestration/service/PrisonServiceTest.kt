package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonDto
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PrisonServiceTest {

  private val visitSchedulerClient = mock<VisitSchedulerClient>()
  private val prisonService: PrisonService = PrisonService(visitSchedulerClient)

  @Test
  fun `works out date range correctly with give prison object`() {
    // Given
    val prisonDto = PrisonDto("HEI", true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))

    // When
    val dateRange = prisonService.getToDaysDateRange(prisonDto)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(LocalDate.now().plusDays(prisonDto.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(LocalDate.now().plusDays(prisonDto.policyNoticeDaysMax.toLong()))
  }
}
