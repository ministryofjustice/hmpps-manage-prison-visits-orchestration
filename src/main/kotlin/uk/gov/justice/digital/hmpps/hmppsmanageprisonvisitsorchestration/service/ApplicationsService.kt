package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerApplicationsClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ChangeApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.CreateApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.VisitSchedulerCreateApplicationDto

@Service
class ApplicationsService(
  private val visitSchedulerApplicationsClient: VisitSchedulerApplicationsClient,
  private val authenticationHelperService: AuthenticationHelperService,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createInitialApplication(createApplicationDto: CreateApplicationDto): ApplicationDto? {
    return visitSchedulerApplicationsClient.createInitialApplication(VisitSchedulerCreateApplicationDto(createApplicationDto, authenticationHelperService.currentUserName))
  }

  fun changeIncompleteApplication(applicationReference: String, changeApplicationDto: ChangeApplicationDto): ApplicationDto? {
    return visitSchedulerApplicationsClient.changeIncompleteApplication(applicationReference, changeApplicationDto)
  }

  fun createApplicationForAnExistingVisit(
    visitReference: String,
    createApplicationDto: CreateApplicationDto,
  ): ApplicationDto? {
    return visitSchedulerApplicationsClient.createApplicationForAnExistingVisit(visitReference, createApplicationDto)
  }
}
