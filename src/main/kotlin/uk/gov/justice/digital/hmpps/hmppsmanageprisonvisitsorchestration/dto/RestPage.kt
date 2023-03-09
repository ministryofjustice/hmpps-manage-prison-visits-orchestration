package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@JsonIgnoreProperties(ignoreUnknown = true, value = ["pageable"])
class RestPage<T> @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
  @JsonProperty("content") content: List<T>?,
  @JsonProperty("number") page: Int,
  @JsonProperty("size") size: Int,
  @JsonProperty("totalElements") total: Long,
) : PageImpl<T>(content as MutableList<T>, PageRequest.of(page, size), total)
