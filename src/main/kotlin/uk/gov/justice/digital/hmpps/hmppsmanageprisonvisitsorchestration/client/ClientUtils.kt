package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client

import org.springframework.web.reactive.function.client.WebClientResponseException

class ClientUtils {
  companion object {
    fun isNotFoundError(e: Throwable?) = e is WebClientResponseException && e.statusCode.value() == 404

    fun isUnprocessableEntityError(e: Throwable?) = e is WebClientResponseException && e.statusCode.value() == 422
  }
}
