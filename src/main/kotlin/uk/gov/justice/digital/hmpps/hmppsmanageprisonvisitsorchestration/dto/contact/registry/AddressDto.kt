package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "An address")
data class AddressDto(
  @param:Schema(description = "Address Type", example = "BUS", required = false) val addressType: String? = null,
  @param:Schema(description = "Flat", example = "3B", required = false) val flat: String? = null,
  @param:Schema(description = "Premise", example = "Liverpool Prison", required = false) val premise: String? = null,
  @param:Schema(description = "Street", example = "Slinn Street", required = false) val street: String? = null,
  @param:Schema(description = "Locality", example = "Brincliffe", required = false) val locality: String? = null,
  @param:Schema(description = "Town/City", example = "Liverpool", required = false) val town: String? = null,
  @param:Schema(description = "Postal Code", example = "LI1 5TH", required = false) val postalCode: String? = null,
  @param:Schema(description = "County", example = "HEREFORD", required = false) val county: String? = null,
  @param:Schema(description = "Country", example = "ENG", required = false) val country: String? = null,
  @param:Schema(description = "Additional Information", example = "This is a comment text", required = false) val comment: String? = null,
  @param:Schema(description = "Primary Address", example = "Y", required = true) val primary: Boolean,
  @param:Schema(description = "No Fixed Address", example = "N", required = true) val noFixedAddress: Boolean,
  @param:Schema(description = "Date Added", example = "2000-10-31", required = false) val startDate: LocalDate? = null,
  @param:Schema(description = "Date ended", example = "2000-10-31", required = false) val endDate: LocalDate? = null,
  @param:Schema(description = "The phone number associated with the address", required = false) val phones: List<TelephoneDto> = listOf(),
  @param:Schema(description = "The address usages/types", required = false) val addressUsages: List<AddressUsageDto> = listOf(),
)
