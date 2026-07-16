package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitSchedulerPrisonDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.CurrentDateUtils
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.utils.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PrisonServiceTest {
  private val visitSchedulerClient = mock<VisitSchedulerClient>()
  private val prisonRegisterClient = mock<PrisonRegisterClient>()
  private val currentDateUtils = mock<CurrentDateUtils>()
  private val dateUtils = DateUtils(currentDateUtils)
  private val excludeDatesService = mock<ExcludeDatesService>()
  private val prisonService = PrisonService(visitSchedulerClient, prisonRegisterClient, dateUtils, excludeDatesService)

  private val today = LocalDate.of(2026, 7, 16)

  @Test
  fun `getToDaysBookableDateRange uses requested user type client when present`() {
    val publicClient = prisonClient(UserType.PUBLIC, policyNoticeDaysMin = 5, policyNoticeDaysMax = 30)
    val staffClient = prisonClient(UserType.STAFF, policyNoticeDaysMin = 1, policyNoticeDaysMax = 10)
    whenever(currentDateUtils.getCurrentDate()).thenReturn(today)
    whenever(visitSchedulerClient.getPrison("MDI")).thenReturn(prison(clients = listOf(staffClient, publicClient)))

    val dateRange = prisonService.getToDaysBookableDateRange(prisonCode = "MDI", userType = UserType.PUBLIC)

    assertThat(dateRange.fromDate).isEqualTo(today.plusDays(6))
    assertThat(dateRange.toDate).isEqualTo(today.plusDays(30))
  }

  @Test
  fun `getToDaysBookableDateRange falls back to staff client when requested user type client is not present`() {
    val staffClient = prisonClient(UserType.STAFF, policyNoticeDaysMin = 2, policyNoticeDaysMax = 28)
    whenever(currentDateUtils.getCurrentDate()).thenReturn(today)
    whenever(visitSchedulerClient.getPrison("MDI")).thenReturn(prison(clients = listOf(staffClient)))

    val dateRange = prisonService.getToDaysBookableDateRange(prisonCode = "MDI", userType = UserType.PUBLIC)

    assertThat(dateRange.fromDate).isEqualTo(today.plusDays(3))
    assertThat(dateRange.toDate).isEqualTo(today.plusDays(28))
  }

  @Test
  fun `getToDaysBookableDateRange throws IllegalStateException when requested and staff clients are not present - PRISONER`() {
    val prisonerClient = prisonClient(UserType.PRISONER, policyNoticeDaysMin = 3, policyNoticeDaysMax = 21)
    whenever(visitSchedulerClient.getPrison("MDI")).thenReturn(prison(clients = listOf(prisonerClient)))

    val exception = assertThrows<IllegalStateException> {
      prisonService.getToDaysBookableDateRange(prisonCode = "MDI", userType = UserType.PUBLIC)
    }

    assertThat(exception.message).isEqualTo("No client found for prison MDI and user type PUBLIC")
  }

  @Test
  fun `getToDaysBookableDateRange throws IllegalStateException when requested and staff clients are not present - SYSTEM`() {
    val prisonerClient = prisonClient(UserType.SYSTEM, policyNoticeDaysMin = 3, policyNoticeDaysMax = 21)
    whenever(visitSchedulerClient.getPrison("MDI")).thenReturn(prison(clients = listOf(prisonerClient)))

    val exception = assertThrows<IllegalStateException> {
      prisonService.getToDaysBookableDateRange(prisonCode = "MDI", userType = UserType.PUBLIC)
    }

    assertThat(exception.message).isEqualTo("No client found for prison MDI and user type PUBLIC")
  }

  private fun prison(clients: List<PrisonUserClientDto>) = VisitSchedulerPrisonDto(
    code = "MDI",
    active = true,
    policyNoticeDaysMin = 1,
    policyNoticeDaysMax = 28,
    maxTotalVisitors = 6,
    maxAdultVisitors = 3,
    maxChildVisitors = 3,
    adultAgeYears = 18,
    weekStartDay = DayOfWeek.MONDAY,
    remandVisitLimitPerWeek = 1,
    clients = clients,
  )

  private fun prisonClient(
    userType: UserType,
    policyNoticeDaysMin: Int,
    policyNoticeDaysMax: Int,
  ) = PrisonUserClientDto(
    userType = userType,
    policyNoticeDaysMin = policyNoticeDaysMin,
    policyNoticeDaysMax = policyNoticeDaysMax,
    active = true,
  )
}
