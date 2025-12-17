package uk.gov.justice.digital.hmpps.orchestration.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import uk.gov.justice.digital.hmpps.orchestration.dto.prison.register.PrisonRegisterContactDetailsDto
import uk.gov.justice.digital.hmpps.orchestration.dto.prison.register.PrisonRegisterPrisonDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.PrisonUserClientDto
import uk.gov.justice.digital.hmpps.orchestration.dto.visit.scheduler.VisitSchedulerPrisonDto

@Schema(description = "Prison dto")
data class PrisonDto(

  @param:Schema(description = "prison code", example = "BHI", required = true)
  val code: String,

  @param:Schema(description = "prison name", example = "HMP Hewell", required = true)
  val prisonName: String,

  @param:Schema(description = "is prison active", example = "true", required = true)
  val active: Boolean = false,

  @param:Schema(description = "minimum number of days notice from the current date to booked a visit", example = "2", required = true)
  val policyNoticeDaysMin: Int,

  @param:Schema(description = "maximum number of days notice from the current date to booked a visit", example = "28", required = true)
  val policyNoticeDaysMax: Int,

  @param:Schema(description = "Max number of total visitors")
  @field:Min(1)
  val maxTotalVisitors: Int,

  @param:Schema(description = "Max number of adults")
  @field:Min(1)
  val maxAdultVisitors: Int,

  @param:Schema(description = "Max number of children")
  @field:Min(0)
  val maxChildVisitors: Int,

  @param:Schema(description = "Age of adults in years")
  val adultAgeYears: Int,

  @param:Schema(description = "Contact email address of prison", example = "example@example.com", required = false)
  val emailAddress: String?,

  @param:Schema(description = "Contact number of prison", required = false)
  val phoneNumber: String?,

  @param:Schema(description = "Web address of prison", required = false)
  val webAddress: String?,

  @param:Schema(description = "prison user client", required = false)
  val clients: List<PrisonUserClientDto> = listOf(),
) {
  constructor(visitSchedulerPrisonDto: VisitSchedulerPrisonDto, prisonRegisterPrisonDto: PrisonRegisterPrisonDto, prisonRegisterContactDetailsDto: PrisonRegisterContactDetailsDto) : this(
    code = visitSchedulerPrisonDto.code,
    prisonName = prisonRegisterPrisonDto.prisonName,
    active = visitSchedulerPrisonDto.active,
    policyNoticeDaysMin = visitSchedulerPrisonDto.policyNoticeDaysMin,
    policyNoticeDaysMax = visitSchedulerPrisonDto.policyNoticeDaysMax,
    maxTotalVisitors = visitSchedulerPrisonDto.maxTotalVisitors,
    maxAdultVisitors = visitSchedulerPrisonDto.maxAdultVisitors,
    maxChildVisitors = visitSchedulerPrisonDto.maxChildVisitors,
    adultAgeYears = visitSchedulerPrisonDto.adultAgeYears,
    clients = visitSchedulerPrisonDto.clients,

    emailAddress = prisonRegisterContactDetailsDto.emailAddress,
    phoneNumber = prisonRegisterContactDetailsDto.phoneNumber,
    webAddress = prisonRegisterContactDetailsDto.webAddress,
  )
}
