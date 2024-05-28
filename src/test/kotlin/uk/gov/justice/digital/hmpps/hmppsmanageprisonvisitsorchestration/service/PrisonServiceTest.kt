package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PrisonServiceTest {

  private val visitSchedulerClient = mock<VisitSchedulerClient>()
  private val pisonRegisterClient = mock<PrisonRegisterClient>()
  var prisonService: PrisonService = PrisonService(visitSchedulerClient, pisonRegisterClient)

  @Test
  fun `works out date range correctly with give prison object`() {
    // Given
    val visitSchedulerPrisonDto = VisitSchedulerPrisonDto("HEI", true, 2, 28, 6, 3, 3, 18, setOf(LocalDate.now()))

    // When
    val dateRange = prisonService.getToDaysDateRange(visitSchedulerPrisonDto)

    // Then
    Assertions.assertThat(dateRange.fromDate).isEqualTo(LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMin.toLong()))
    Assertions.assertThat(dateRange.toDate).isEqualTo(LocalDate.now().plusDays(visitSchedulerPrisonDto.policyNoticeDaysMax.toLong()))
  }
}
