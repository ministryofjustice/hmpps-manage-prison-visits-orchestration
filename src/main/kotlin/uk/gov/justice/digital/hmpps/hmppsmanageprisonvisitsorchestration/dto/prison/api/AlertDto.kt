package uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.prison.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Schema(description = "Alert")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AlertDto (
    @Hidden
    @JsonIgnore
    val additionalProperties: Map<String, Any>? = null,

    @Schema(required = true, description = "Alert Id", example = "1")
    @JsonProperty("alertId")
    val alertId: @NotNull Long? = null,

    @Schema(required = true, description = "Offender booking id.", example = "14")
    @JsonProperty("bookingId")
    val bookingId: @NotNull Long? = null,

    @Schema(required = true, description = "Offender Unique Reference", example = "G3878UK")
    @JsonProperty("offenderNo")
    val offenderNo: @NotBlank String? = null,

    @Schema(required = true, description = "Alert Type", example = "X")
    @JsonProperty("alertType")
    val alertType: @NotBlank String? = null,

    @Schema(required = true, description = "Alert Type Description", example = "Security")
    @JsonProperty("alertTypeDescription")
    val alertTypeDescription: @NotBlank String? = null,

    @Schema(required = true, description = "Alert Code", example = "XER")
    @JsonProperty("alertCode")
    val alertCode: @NotBlank String? = null,

    @Schema(required = true, description = "Alert Code Description", example = "Escape Risk")
    @JsonProperty("alertCodeDescription")
    val alertCodeDescription: @NotBlank String? = null,

    @Schema(required = true, description = "Alert comments", example = "Profession lock pick.")
    @JsonProperty("comment")
    val comment: @NotBlank String? = null,

    @Schema(
        required = true,
        description = "Date of the alert, which might differ to the date it was created",
        example = "2019-08-20"
    )
    @JsonProperty("dateCreated")
    val dateCreated: @NotNull LocalDate? = null,

    @Schema(description = "Date the alert expires", example = "2020-08-20")
    @JsonProperty("dateExpires")
    val dateExpires: LocalDate? = null,

    @Schema(required = true, description = "True / False based on presence of expiry date", example = "true")
    @JsonProperty("expired")
    val expired: @NotNull Boolean = false,

    @Schema(required = true, description = "True / False based on alert status", example = "false")
    @JsonProperty("active")
    val active: @NotNull Boolean = false,

    @Schema(description = "First name of the user who added the alert", example = "John")
    @JsonProperty("addedByFirstName")
    val addedByFirstName: String? = null,

    @Schema(description = "Last name of the user who added the alert", example = "Smith")
    @JsonProperty("addedByLastName")
    val addedByLastName: String? = null,

    @Schema(description = "First name of the user who last modified the alert", example = "John")
    @JsonProperty("expiredByFirstName")
    val expiredByFirstName: String? = null,

    @Schema(description = "Last name of the user who last modified the alert", example = "Smith")
    @JsonProperty("expiredByLastName")
    val expiredByLastName: String? = null
)