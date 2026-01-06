package uk.gov.justice.digital.hmpps.prison.visits.orchestration.dto.govuk.holidays

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class HolidaysDto(
  @param:Schema(description = "Bank holidays only for England and Wales - as retrieved from Gov.UK endpoint - https://www.gov.uk/bank-holidays.json")
  @param:JsonProperty("england-and-wales")
  val englandAndWalesHolidays: HolidayEventByDivisionDto,
)
