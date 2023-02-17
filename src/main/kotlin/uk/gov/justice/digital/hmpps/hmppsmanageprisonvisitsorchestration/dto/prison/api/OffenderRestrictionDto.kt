package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import javax.validation.constraints.NotBlank

@Schema(description = "Offender restriction")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderRestrictionDto (
    @Schema(required = true, description = "restriction id")
    val restrictionId: Long? = null,

    @Schema(description = "Restriction comment text")
    val comment: String? = null,

    @Schema(required = true, description = "code of restriction type")
    val restrictionType: String? = null,

    @Schema(required = true, description = "description of restriction type")
    val restrictionTypeDescription: @NotBlank String? = null,

    @Schema(required = true, description = "Date from which the restrictions applies", example = "1980-01-01")
    val startDate: LocalDate? = null,

    @Schema(description = "Date restriction applies to, or indefinitely if null", example = "1980-01-01")
    val expiryDate: LocalDate? = null,

    @Schema(required = true, description = "true if restriction is within the start date and optional expiry date range")
    val active: Boolean = false
)