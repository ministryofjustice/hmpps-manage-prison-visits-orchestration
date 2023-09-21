package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.visitnotification

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events.additionalinfo.VisitorRestrictionChangeInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class VisitorRestrictionChangeNotificationDto(
  @NotBlank
  @JsonProperty("personId")
  val personVisitorId: String,
  @NotBlank
  @JsonProperty("effectiveDate")
  val validFromDate: LocalDate,
  @JsonInclude(Include.NON_NULL)
  @JsonProperty("expiryDate")
  val validToDate: LocalDate? = null,
) {

  constructor(info: VisitorRestrictionChangeInfo) : this(
    info.personVisitorId,
    LocalDate.parse(info.validFromDate, DateTimeFormatter.ISO_DATE),
    info.validToDate?.let { LocalDate.parse(info.validToDate, DateTimeFormatter.ISO_DATE) },
  )
}
