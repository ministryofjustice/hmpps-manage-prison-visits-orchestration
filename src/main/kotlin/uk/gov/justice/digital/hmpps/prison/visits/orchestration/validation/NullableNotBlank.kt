package uk.gov.justice.digital.hmpps.prison.visits.orchestration.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
@Constraint(validatedBy = [NullableNotBlankValidator::class])
annotation class NullableNotBlank(
  val message: String = "{javax.validation.constraints.NotBlank.message}",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class NullableNotBlankValidator : ConstraintValidator<NullableNotBlank, String> {
  override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
    if (value == null) return true
    return value.isNotBlank()
  }
}
