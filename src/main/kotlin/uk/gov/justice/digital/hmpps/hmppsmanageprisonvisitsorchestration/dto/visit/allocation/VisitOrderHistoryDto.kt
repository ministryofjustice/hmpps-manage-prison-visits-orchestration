package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.allocation

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class VisitOrderHistoryDto(
  @param:Schema(description = "nomsNumber of the prisoner", example = "AA123456", required = true)
  val prisonerId: String,

  @param:Schema(description = "Visit Order History Type", example = "VO_ALLOCATION", required = true)
  val visitOrderHistoryType: String,

  @param:Schema(description = "Visit order history created data and time", example = "2018-12-01T13:45:00", required = true)
  val createdTimeStamp: LocalDateTime,

  @param:Schema(description = "VO balance after the visit order event", example = "5", required = true)
  val voBalance: Int,

  @param:Schema(description = "VO balance change", example = "-1", required = true)
  var voBalanceChange: Int? = null,

  @param:Schema(description = "PVO balance after the visit order event", example = "5", required = true)
  val pvoBalance: Int,

  @param:Schema(description = "PVO balance change", example = "-1", required = true)
  var pvoBalanceChange: Int? = null,

  @param:Schema(description = "Username for who triggered the event, SYSTEM if system generated or STAFF full name if STAFF event (e.g. manual adjustment)", example = "SYSTEM", required = true)
  var userName: String,

  @param:Schema(description = "Comment added by STAFF, null if SYSTEM event or if no comment was entered by STAFF", required = false)
  val comment: String? = null,

  @param:Schema(description = "Key, value combination of attributes", required = true)
  val attributes: Map<String, String> = mapOf(),
)
