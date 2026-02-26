package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.ChangeApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.CreateApplicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.application.VisitSchedulerCreateApplicationDto
import java.time.Duration

const val APPLICATION_CONTROLLER_PATH: String = "/visits/application"

@Component
class VisitSchedulerApplicationsClient(
  @param:Qualifier("visitSchedulerWebClient") private val webClient: WebClient,
  @param:Value("\${visit-scheduler.api.timeout:10s}") val apiTimeout: Duration,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createInitialApplication(createApplicationDto: VisitSchedulerCreateApplicationDto): ApplicationDto? = webClient.post()
    .uri("$APPLICATION_CONTROLLER_PATH//slot/reserve")
    .body(BodyInserters.fromValue(createApplicationDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<ApplicationDto>().block(apiTimeout)

  fun changeIncompleteApplication(reference: String, changeApplicationDto: ChangeApplicationDto): ApplicationDto? = webClient.put()
    .uri("$APPLICATION_CONTROLLER_PATH/$reference/slot/change")
    .body(BodyInserters.fromValue(changeApplicationDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<ApplicationDto>().block(apiTimeout)

  fun createApplicationForAnExistingVisit(bookingReference: String, createApplicationDto: CreateApplicationDto): ApplicationDto? = webClient.put()
    .uri("$APPLICATION_CONTROLLER_PATH/$bookingReference/change")
    .body(BodyInserters.fromValue(createApplicationDto))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<ApplicationDto>().block(apiTimeout)
}
