package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@ExtendWith(SpringExtension::class)
internal class VisitWriteEventListenerServiceTest {
  private val objectMapper = jacksonObjectMapper()
  private val visitWriteEventListenerService = VisitWriteEventListenerService(objectMapper)

  @Test
  fun `will process a visit write create event`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "VisitCreated",
      "description" : null,
      "messageAttributes" : {
        "prisonerId" : "A1234AB",
        "prisonId" : "MDI",
        "clientVisitReference" : "123456",
        "visitRoom" : "A1",
        "visitType" : "SOCIAL",
        "visitStatus" : "BOOKED",
        "visitRestriction" : "OPEN",
        "startTimestamp" : "2020-12-04T10:42:43",
        "endTimestamp" : "2020-12-04T10:42:43",
        "createDateTime" : "2020-12-04T10:42:43",
        "visitors" : [ {
          "nomisPersonId" : 3,
          "visitContact" : true
        } ],
        "actionedBy" : "automated-test-client"
      },
      "who" : "automated-test-client"
    }
    """

    assertDoesNotThrow {
      visitWriteEventListenerService.onEventReceived(message).get()
    }
  }

  @Test
  fun `will process a visit write update event`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "VisitUpdated",
      "description" : null,
      "messageAttributes" : {
        "prisonerId" : "A1234AB",
        "prisonId" : "MDI",
        "clientVisitReference" : "123456",
        "visitRoom" : "A1",
        "visitType" : "SOCIAL",
        "visitStatus" : "BOOKED",
        "visitRestriction" : "OPEN",
        "startTimestamp" : "2020-12-04T10:42:43",
        "endTimestamp" : "2020-12-04T10:42:43",
        "createDateTime" : "2020-12-04T10:42:43",
        "visitors" : [ {
          "nomisPersonId" : 3,
          "visitContact" : true
        } ],
        "actionedBy" : "automated-test-client"
      },
      "who" : "automated-test-client"
    }
    """

    assertDoesNotThrow {
      visitWriteEventListenerService.onEventReceived(message).get()
    }
  }

  @Test
  fun `will process a visit write cancelled event`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "VisitCancelled",
      "description" : null,
      "messageAttributes" : {
        "prisonerId" : "A1234AB",
        "prisonId" : "MDI",
        "clientVisitReference" : "123456",
        "visitRoom" : "A1",
        "visitType" : "SOCIAL",
        "visitStatus" : "BOOKED",
        "visitRestriction" : "OPEN",
        "startTimestamp" : "2020-12-04T10:42:43",
        "endTimestamp" : "2020-12-04T10:42:43",
        "createDateTime" : "2020-12-04T10:42:43",
        "visitors" : [ {
          "nomisPersonId" : 3,
          "visitContact" : true
        } ],
        "actionedBy" : "automated-test-client"
      },
      "who" : "automated-test-client"
    }
    """

    assertDoesNotThrow {
      visitWriteEventListenerService.onEventReceived(message).get()
    }
  }

  @Test
  fun `will throw an an exception when invalid visit write event passed in`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "InvalidEventType",
      "description" : null,
      "messageAttributes" : {
        "prisonerId" : "A1234AB",
        "prisonId" : "MDI",
        "clientVisitReference" : "123456",
        "visitRoom" : "A1",
        "visitType" : "SOCIAL",
        "visitStatus" : "BOOKED",
        "visitRestriction" : "OPEN",
        "startTimestamp" : "2020-12-04T10:42:43",
        "endTimestamp" : "2020-12-04T10:42:43",
        "createDateTime" : "2020-12-04T10:42:43",
        "visitors" : [ {
          "nomisPersonId" : 3,
          "visitContact" : true
        } ],
        "actionedBy" : "automated-test-client"
      },
      "who" : "automated-test-client"
    }
    """

    val exception = assertThrows<Exception>{
        visitWriteEventListenerService.onEventReceived(message).get()
    }
    assertThat(exception.message).contains("Cannot process event of type InvalidEventType")
  }
}