package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.VisitDto
import uk.gov.service.notify.NotificationClient
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
class NotificationService(
  val client: NotificationClient,
  val prisonRegisterClient: PrisonRegisterClient,
  @Value("\${notify.apikey:}") private val notifyApiKey: String,
  @Value("\${notify.template-id.visit-booking:}") private val visitBookingTemplateId: String,
  @Value("\${notify.template-id.visit-update:}") private val visitUpdateTemplateId: String,
  @Value("\${notify.template-id.visit-cancel:}") private val visitCancelTemplateId: String,
) {
  private final val SMS_DATE_PATTERN = "dd MMMM yyyy"
  final val SMS_TIME_PATTERN = "hh:mm a"
  final val SMS_DAYOFWEEK_PATTERN = "EEEE"

  enum class NotificationEvent(val description: String) {
    VISIT_BOOKING("Booking"),
    VISIT_UPDATE("Update"),
    VISIT_CANCEL("Cancel"),
  }

  fun sendConfirmation(notificationEvent: NotificationEvent, visit: VisitDto) {
    visit.visitContact?.let { visitContact ->
      when (notificationEvent) {
        NotificationEvent.VISIT_BOOKING -> {
          sendSms(
            visitBookingTemplateId,
            visitContact.telephone,
            mapOf(
              "prison" to getPrisonDetails(visit),
              "time" to getFormattedVisitTime(visit.startTimestamp.toLocalTime()),
              "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
              "date" to getFormattedVisitDate(visit.startTimestamp.toLocalDate()),
              "ref number" to visit.reference,
            ),
          )
        }
        NotificationEvent.VISIT_UPDATE -> {
          sendSms(
            visitUpdateTemplateId,
            visitContact.telephone,
            mapOf(
              "prison" to getPrisonDetails(visit),
              "time" to getFormattedVisitTime(visit.startTimestamp.toLocalTime()),
              "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
              "date" to getFormattedVisitDate(visit.startTimestamp.toLocalDate()),
              "ref number" to visit.reference,
            ),
          )
        }
        NotificationEvent.VISIT_CANCEL -> {
          // TODO - this is blocked till story to save a prison's phone number is implemented
          sendSms(visitCancelTemplateId, visitContact.telephone, emptyMap())
        }
      }
    }
  }

  fun getFormattedVisitDate(visitDate: LocalDate): String {
    return visitDate.format(DateTimeFormatter.ofPattern(SMS_DATE_PATTERN))
  }

  fun getFormattedVisitTime(visitStartTime: LocalTime): String {
    return visitStartTime.format(DateTimeFormatter.ofPattern(SMS_TIME_PATTERN))
  }

  fun getFormattedDayOfWeek(visitDate: LocalDate): String {
    return visitDate.format(DateTimeFormatter.ofPattern(SMS_DAYOFWEEK_PATTERN))
  }

  fun getPrisonDetails(visit: VisitDto): String {
    // get prison details from prison register
    val prison = prisonRegisterClient.getPrison(visit.prisonCode)
    return prison?.prisonName ?: visit.prisonCode
  }

  fun sendSms(templateID: String, phoneNumber: String, personalisation: Map<String, String>) {
    client.sendSms(templateID, phoneNumber, personalisation, null)
  }
}
