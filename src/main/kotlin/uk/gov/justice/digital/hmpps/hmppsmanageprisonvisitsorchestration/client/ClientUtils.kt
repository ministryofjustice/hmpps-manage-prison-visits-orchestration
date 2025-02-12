package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

class ClientUtils {
  companion object {
    fun isNotFoundError(e: Throwable?) = e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND

    fun isUnprocessableEntityError(e: Throwable?) = e is WebClientResponseException && e.statusCode == HttpStatus.UNPROCESSABLE_ENTITY
  }
}
