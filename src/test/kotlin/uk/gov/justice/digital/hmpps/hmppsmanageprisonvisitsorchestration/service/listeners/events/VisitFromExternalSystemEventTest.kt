package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.service.listeners.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CancelVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.CreateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.UpdateVisitFromExternalSystemDto
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.ApplicationMethodType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.visit.scheduler.enums.UserType
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
        "actionedBy" to "external-user",
        "applicationMethodType" to "PHONE",
        "userType" to "STAFF",
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
      assertThat(it.actionedBy).isEqualTo("external-user")
      assertThat(it.applicationMethodType).isEqualTo(ApplicationMethodType.PHONE)
      assertThat(it.userType).isEqualTo(UserType.STAFF)
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
  fun `SOCIAL_VIDEO visit type string in message attributes is mapped for create and update`() {
    val attributes = mapOf(
      "prisonerId" to "A1243B",
      "prisonId" to "MKI",
      "clientName" to "client-name",
      "clientVisitReference" to "client-visit-reference",
      "actionedBy" to "A1243B",
      "applicationMethodType" to "BY_PRISONER",
      "userType" to "PRISONER",
      "visitRoom" to "A1",
      "visitType" to "SOCIAL_VIDEO",
      "visitRestriction" to "OPEN",
      "startTimestamp" to LocalDateTime.now().toString(),
      "endTimestamp" to LocalDateTime.now().plusHours(1).toString(),
      "visitContact" to mapOf("name" to "John Smith"),
      "createDateTime" to LocalDateTime.now().toString(),
      "visitReference" to "ab-cd-ef-gh",
    )
    val event = VisitFromExternalSystemEvent(messageId = "message-id", eventType = "VisitCreated", messageAttributes = attributes)

    assertThat(event.toCreateVisitFromExternalSystemDto().visitType).isEqualTo(VisitType.SOCIAL_VIDEO)
    assertThat(event.toUpdateVisitFromExternalSystemDto().visitType).isEqualTo(VisitType.SOCIAL_VIDEO)
  }

  @Test
  fun `get a UpdateVisitFromExternalSystemDto from message attributes`() {
    val visitFromExternalSystemEvent = VisitFromExternalSystemEvent(
      messageId = "message-id",
      eventType = "VisitUpdated",
      messageAttributes = mapOf(
        "visitReference" to "v9-d7-ed-7u",
        "actionedBy" to "A1243B",
        "applicationMethodType" to "BY_PRISONER",
        "userType" to "PRISONER",
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
      assertThat(it.actionedBy).isEqualTo("A1243B")
      assertThat(it.applicationMethodType).isEqualTo(ApplicationMethodType.BY_PRISONER)
      assertThat(it.userType).isEqualTo(UserType.PRISONER)
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

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `create and update accept legacy payloads with omitted or null audit metadata`(explicitNull: Boolean) {
    val metadataFields = setOf("actionedBy", "applicationMethodType", "userType")
    val event = eventWithAuditMetadata().let {
      val legacyAttributes = it.messageAttributes - metadataFields
      it.copy(messageAttributes = if (explicitNull) legacyAttributes + metadataFields.associateWith { null } else legacyAttributes)
    }

    val createDto = event.toCreateVisitFromExternalSystemDto()
    assertThat(createDto.actionedBy).isNull()
    assertThat(createDto.applicationMethodType).isNull()
    assertThat(createDto.userType).isNull()
    val updateDto = event.toUpdateVisitFromExternalSystemDto()
    assertThat(updateDto.actionedBy).isNull()
    assertThat(updateDto.applicationMethodType).isNull()
    assertThat(updateDto.userType).isNull()
  }

  @ParameterizedTest
  @ValueSource(strings = ["actionedBy", "applicationMethodType", "userType"])
  fun `create and update preserve individually supplied audit metadata`(field: String) {
    val event = eventWithAuditMetadata().let {
      it.copy(messageAttributes = (it.messageAttributes + ("userType" to "PRISONER")) - (setOf("actionedBy", "applicationMethodType", "userType") - field))
    }

    val createDto = event.toCreateVisitFromExternalSystemDto()
    assertThat(createDto.actionedBy).isEqualTo(if (field == "actionedBy") "external-user" else null)
    assertThat(createDto.applicationMethodType).isEqualTo(if (field == "applicationMethodType") ApplicationMethodType.PHONE else null)
    assertThat(createDto.userType).isEqualTo(if (field == "userType") UserType.PRISONER else null)
    val updateDto = event.toUpdateVisitFromExternalSystemDto()
    assertThat(updateDto.actionedBy).isEqualTo(createDto.actionedBy)
    assertThat(updateDto.applicationMethodType).isEqualTo(createDto.applicationMethodType)
    assertThat(updateDto.userType).isEqualTo(createDto.userType)
  }

  @ParameterizedTest
  @ValueSource(strings = ["STAFF", "PUBLIC"])
  fun `create and update preserve staff and public actor metadata`(userType: String) {
    val event = eventWithAuditMetadata().let {
      it.copy(messageAttributes = it.messageAttributes + ("userType" to userType))
    }

    val createDto = event.toCreateVisitFromExternalSystemDto()
    assertThat(createDto.actionedBy).isEqualTo("external-user")
    assertThat(createDto.userType).isEqualTo(UserType.valueOf(userType))
    val updateDto = event.toUpdateVisitFromExternalSystemDto()
    assertThat(updateDto.actionedBy).isEqualTo("external-user")
    assertThat(updateDto.userType).isEqualTo(UserType.valueOf(userType))
  }

  @ParameterizedTest
  @ValueSource(strings = ["applicationMethodType", "userType"])
  fun `create and update reject unknown audit enum values`(field: String) {
    val event = eventWithAuditMetadata().let {
      it.copy(messageAttributes = it.messageAttributes + (field to "UNKNOWN"))
    }

    assertThat(assertThrows<InvalidFormatException> { event.toCreateVisitFromExternalSystemDto() }.message).contains(field)
    assertThat(assertThrows<InvalidFormatException> { event.toUpdateVisitFromExternalSystemDto() }.message).contains(field)
  }

  @ParameterizedTest
  @ValueSource(strings = ["STAFF", "PUBLIC"])
  fun `username alias is converted and serialized as actionedBy for create and update`(userType: String) {
    val event = eventWithAuditMetadata().let {
      it.copy(messageAttributes = (it.messageAttributes - "actionedBy") + mapOf("username" to "external-user", "userType" to userType))
    }
    val mapper = jacksonObjectMapper()

    val createDto = event.toCreateVisitFromExternalSystemDto()
    assertThat(createDto.actionedBy).isEqualTo("external-user")
    val createJson = mapper.readTree(mapper.writeValueAsString(createDto))
    assertThat(createJson["actionedBy"].asString()).isEqualTo("external-user")
    assertThat(createJson.has("username")).isFalse()

    val updateDto = event.toUpdateVisitFromExternalSystemDto()
    assertThat(updateDto.actionedBy).isEqualTo("external-user")
    val updateJson = mapper.readTree(mapper.writeValueAsString(updateDto))
    assertThat(updateJson["actionedBy"].asString()).isEqualTo("external-user")
    assertThat(updateJson.has("username")).isFalse()
  }

  private fun eventWithAuditMetadata() = VisitFromExternalSystemEvent(
    messageId = "message-id",
    eventType = "VisitCreated",
    messageAttributes = mapOf(
      "prisonerId" to "A1243B",
      "prisonId" to "MKI",
      "clientName" to "client-name",
      "clientVisitReference" to "client-visit-reference",
      "visitReference" to "v9-d7-ed-7u",
      "visitRoom" to "A1",
      "visitType" to "SOCIAL",
      "visitRestriction" to "OPEN",
      "startTimestamp" to "2026-11-09T14:00:00",
      "endTimestamp" to "2026-11-09T15:00:00",
      "visitContact" to mapOf("name" to "John Smith"),
      "createDateTime" to "2026-11-08T12:00:00",
      "actionedBy" to "external-user",
      "applicationMethodType" to "PHONE",
      "userType" to "STAFF",
    ),
  )
}
