package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertInstanceOf
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CreateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.UpdateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitNoteType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitRestriction
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.VisitType
import java.time.LocalDateTime

class VisitFromExternalSystemEventTest {
  @Test
  fun `get a CreateVisitFromExternalSystemDto from message attributes`() {
    val visitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = "message-id",
      eventType = "VisitCreated",
      messageAttributes = mapOf(
        "prisonerId" to "A1243B",
        "prisonId" to "MKI",
        "clientName" to "client-name",
        "clientVisitReference" to "client-visit-reference",
        "visitRoom" to "A1",
        "visitType" to VisitType.SOCIAL,
        "visitRestriction" to VisitRestriction.OPEN,
        "startTimestamp" to LocalDateTime.now().toString(),
        "endTimestamp" to LocalDateTime.now().plusHours(1).toString(),
        "visitNotes" to listOf(mapOf("type" to VisitNoteType.VISITOR_CONCERN, "text" to "Visitor concern")),
        "visitContact" to mapOf("name" to "John Smith", "telephone" to "01234567890", "email" to "john.smith@example.com"),
        "createDateTime" to LocalDateTime.now().toString(),
        "visitors" to listOf(mapOf("nomisPersonId" to 1234, "visitContact" to true)),
        "visitorSupport" to mapOf("description" to "Visual impairment"),
      ),
    )

    val createVisitFromExternalSystemDto = assertDoesNotThrow { visitFromExternalSystemEvent.toCreateVisitFromExternalSystemDto() }

    assertInstanceOf<CreateVisitFromExternalSystemDto>(createVisitFromExternalSystemDto)
    createVisitFromExternalSystemDto.let {
      assertThat(it.prisonerId).isEqualTo(visitFromExternalSystemEvent.messageAttributes["prisonerId"])
      assertThat(it.prisonId).isEqualTo(visitFromExternalSystemEvent.messageAttributes["prisonId"])
      assertThat(it.clientName).isEqualTo(visitFromExternalSystemEvent.messageAttributes["clientName"])
      assertThat(it.clientVisitReference).isEqualTo(visitFromExternalSystemEvent.messageAttributes["clientVisitReference"])
      assertThat(it.visitRoom).isEqualTo(visitFromExternalSystemEvent.messageAttributes["visitRoom"])
      assertThat(it.visitType).isEqualTo(visitFromExternalSystemEvent.messageAttributes["visitType"])
      assertThat(it.visitRestriction).isEqualTo(visitFromExternalSystemEvent.messageAttributes["visitRestriction"])
      assertThat(it.startTimestamp).isEqualTo(LocalDateTime.parse(visitFromExternalSystemEvent.messageAttributes["startTimestamp"] as String))
      assertThat(it.endTimestamp).isEqualTo(LocalDateTime.parse(visitFromExternalSystemEvent.messageAttributes["endTimestamp"] as String))
      assertThat(it.createDateTime).isEqualTo(LocalDateTime.parse(visitFromExternalSystemEvent.messageAttributes["createDateTime"] as String))

      (visitFromExternalSystemEvent.messageAttributes["visitNotes"] as List<*>).let { visitNotes ->
        assertThat(it.visitNotes.size).isEqualTo(visitNotes.size)
        for (i in visitNotes.indices) {
          (visitNotes[i] as Map<*, *>).let { visitNote ->
            assertThat(it.visitNotes[i].type).isEqualTo(visitNote["type"])
            assertThat(it.visitNotes[i].text).isEqualTo(visitNote["text"])
          }
        }
      }

      (visitFromExternalSystemEvent.messageAttributes["visitContact"] as Map<*, *>).let { visitContact ->
        assertThat(it.visitContact.name).isEqualTo(visitContact["name"])
        assertThat(it.visitContact.telephone).isEqualTo(visitContact["telephone"])
        assertThat(it.visitContact.email).isEqualTo(visitContact["email"])
      }

      (visitFromExternalSystemEvent.messageAttributes["visitors"] as List<*>).let { visitors ->
        val inputVisitors = it.visitors?.toList().orEmpty()
        assertThat(it.visitors?.size).isEqualTo(visitors.size)
        for (i in visitors.indices) {
          (visitors[i] as Map<*, *>).let { visitor ->
            assertThat(inputVisitors[0].nomisPersonId).isEqualTo((visitor["nomisPersonId"] as Int).toLong())
            assertThat(inputVisitors[0].visitContact).isEqualTo(visitor["visitContact"])
          }
        }
      }

      (visitFromExternalSystemEvent.messageAttributes["visitorSupport"] as Map<*, *>).let { visitSupport ->
        assertThat(it.visitorSupport?.description).isEqualTo(visitSupport["description"])
      }
    }
  }

  @Test
  fun `get a UpdateVisitFromExternalSystemDto from message attributes`() {
    val visitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = "message-id",
      eventType = "VisitUpdated",
      messageAttributes = mapOf(
        "visitReference" to "v9-d7-ed-7u",
        "visitRoom" to "A1",
        "visitType" to VisitType.SOCIAL,
        "visitRestriction" to VisitRestriction.OPEN,
        "startTimestamp" to LocalDateTime.now().toString(),
        "endTimestamp" to LocalDateTime.now().plusHours(1).toString(),
        "visitNotes" to listOf(mapOf("type" to VisitNoteType.VISITOR_CONCERN, "text" to "Visitor concern")),
        "visitContact" to mapOf("name" to "John Smith", "telephone" to "01234567890", "email" to "john.smith@example.com"),
        "visitors" to listOf(mapOf("nomisPersonId" to 1234, "visitContact" to true)),
        "visitorSupport" to mapOf("description" to "Visual impairment"),
      ),
    )

    val updateVisitFromExternalSystemDto = assertDoesNotThrow { visitFromExternalSystemEvent.toUpdateVisitFromExternalSystemDto() }

    assertInstanceOf<UpdateVisitFromExternalSystemDto>(updateVisitFromExternalSystemDto)
    updateVisitFromExternalSystemDto.let {
      assertThat(it.visitReference).isEqualTo(visitFromExternalSystemEvent.messageAttributes["visitReference"])
      assertThat(it.visitRoom).isEqualTo(visitFromExternalSystemEvent.messageAttributes["visitRoom"])
      assertThat(it.visitType).isEqualTo(visitFromExternalSystemEvent.messageAttributes["visitType"])
      assertThat(it.visitRestriction).isEqualTo(visitFromExternalSystemEvent.messageAttributes["visitRestriction"])
      assertThat(it.startTimestamp).isEqualTo(LocalDateTime.parse(visitFromExternalSystemEvent.messageAttributes["startTimestamp"] as String))
      assertThat(it.endTimestamp).isEqualTo(LocalDateTime.parse(visitFromExternalSystemEvent.messageAttributes["endTimestamp"] as String))

      (visitFromExternalSystemEvent.messageAttributes["visitNotes"] as List<*>).let { visitNotes ->
        assertThat(it.visitNotes.size).isEqualTo(visitNotes.size)
        for (i in visitNotes.indices) {
          (visitNotes[i] as Map<*, *>).let { visitNote ->
            assertThat(it.visitNotes[i].type).isEqualTo(visitNote["type"])
            assertThat(it.visitNotes[i].text).isEqualTo(visitNote["text"])
          }
        }
      }

      (visitFromExternalSystemEvent.messageAttributes["visitContact"] as Map<*, *>).let { visitContact ->
        assertThat(it.visitContact.name).isEqualTo(visitContact["name"])
        assertThat(it.visitContact.telephone).isEqualTo(visitContact["telephone"])
        assertThat(it.visitContact.email).isEqualTo(visitContact["email"])
      }

      (visitFromExternalSystemEvent.messageAttributes["visitors"] as List<*>).let { visitors ->
        val inputVisitors = it.visitors?.toList().orEmpty()
        assertThat(it.visitors?.size).isEqualTo(visitors.size)
        for (i in visitors.indices) {
          (visitors[i] as Map<*, *>).let { visitor ->
            assertThat(inputVisitors[0].nomisPersonId).isEqualTo((visitor["nomisPersonId"] as Int).toLong())
            assertThat(inputVisitors[0].visitContact).isEqualTo(visitor["visitContact"])
          }
        }
      }

      (visitFromExternalSystemEvent.messageAttributes["visitorSupport"] as Map<*, *>).let { visitSupport ->
        assertThat(it.visitorSupport?.description).isEqualTo(visitSupport["description"])
      }
    }
  }


  @Test
  fun `get a CancelVisitFromExternalSystemDto from message attributes`() {
    val visitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = "message-id",
      eventType = "VisitCancelled",
      messageAttributes = mapOf(
        "visitReference" to "v9-d7-ed-7u",
        "cancelOutcome" to mapOf("outcomeStatus" to "CANCELLATION", "text" to "Whatever"),
        "userType" to "PRISONER",
        "actionedBy" to "A1243B",
      ),
    )

    val cancelVisitFromExternalSystemDto = assertDoesNotThrow { visitFromExternalSystemEvent.toCancelVisitFromExternalSystemDto() }

    assertInstanceOf<CancelVisitFromExternalSystemDto>(cancelVisitFromExternalSystemDto)
    cancelVisitFromExternalSystemDto.let {
      assertThat(it.visitReference).isEqualTo(visitFromExternalSystemEvent.messageAttributes["visitReference"])
      assertThat(it.userType.toString()).isEqualTo(visitFromExternalSystemEvent.messageAttributes["userType"])
      assertThat(it.actionedBy).isEqualTo(visitFromExternalSystemEvent.messageAttributes["actionedBy"])

      (visitFromExternalSystemEvent.messageAttributes["cancelOutcome"] as Map<*, *>).let { cancelOutcome ->
        assertThat(it.cancelOutcome.outcomeStatus.toString()).isEqualTo(cancelOutcome["outcomeStatus"])
        assertThat(it.cancelOutcome.text).isEqualTo(cancelOutcome["text"])
      }
    }
  }
}
