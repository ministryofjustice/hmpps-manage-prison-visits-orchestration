package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.VisitSchedulerApplicationsClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ChangeApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.CreateApplicationDto

@Service
class ApplicationsService(
  private val visitSchedulerApplicationsClient: VisitSchedulerApplicationsClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createInitialApplication(createApplicationDto: CreateApplicationDto): ApplicationDto? {
    return visitSchedulerApplicationsClient.createInitialApplication(createApplicationDto)
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
