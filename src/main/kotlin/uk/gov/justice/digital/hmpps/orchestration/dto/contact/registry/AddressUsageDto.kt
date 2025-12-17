package uk.gov.justice.digital.hmpps.orchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "An Offender's address usage")
class AddressUsageDto(
  @param:Schema(description = "The address usages", example = "HDC", required = false) val addressUsage: String? = null,
  @param:Schema(description = "The address usages description", example = "HDC Address", required = false) val addressUsageDescription: String? = null,
  @param:Schema(description = "Active Flag", example = "true", required = false) val activeFlag: Boolean? = null,
)
