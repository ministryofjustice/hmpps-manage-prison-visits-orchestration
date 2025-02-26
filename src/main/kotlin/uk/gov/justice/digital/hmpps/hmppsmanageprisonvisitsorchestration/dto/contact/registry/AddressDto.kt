package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "An address")
data class AddressDto(
  @Schema(description = "Address Type", example = "BUS", required = false) val addressType: String? = null,
  @Schema(description = "Flat", example = "3B", required = false) val flat: String? = null,
  @Schema(description = "Premise", example = "Liverpool Prison", required = false) val premise: String? = null,
  @Schema(description = "Street", example = "Slinn Street", required = false) val street: String? = null,
  @Schema(description = "Locality", example = "Brincliffe", required = false) val locality: String? = null,
  @Schema(description = "Town/City", example = "Liverpool", required = false) val town: String? = null,
  @Schema(description = "Postal Code", example = "LI1 5TH", required = false) val postalCode: String? = null,
  @Schema(description = "County", example = "HEREFORD", required = false) val county: String? = null,
  @Schema(description = "Country", example = "ENG", required = false) val country: String? = null,
  @Schema(description = "Additional Information", example = "This is a comment text", required = false) val comment: String? = null,
  @Schema(description = "Primary Address", example = "Y", required = true) val primary: Boolean,
  @Schema(description = "No Fixed Address", example = "N", required = true) val noFixedAddress: Boolean,
  @Schema(description = "Date Added", example = "2000-10-31", required = false) val startDate: LocalDate? = null,
  @Schema(description = "Date ended", example = "2000-10-31", required = false) val endDate: LocalDate? = null,
  @Schema(description = "The phone number associated with the address", required = false) val phones: List<TelephoneDto> = listOf(),
  @Schema(description = "The address usages/types", required = false) val addressUsages: List<AddressUsageDto> = listOf(),
)
