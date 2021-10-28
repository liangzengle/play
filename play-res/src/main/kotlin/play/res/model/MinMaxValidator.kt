package play.res.model

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.constraints.PositiveOrZero

internal class PositiveOrZeroValidatorForMinMaxInt : ConstraintValidator<PositiveOrZero, MinMaxInt> {
  override fun isValid(value: MinMaxInt?, context: ConstraintValidatorContext): Boolean {
    if (value == null) {
      return true
    }
    return value.min >= 0 && value.max >= 0
  }
}

internal class PositiveOrZeroValidatorForMinMaxLong : ConstraintValidator<PositiveOrZero, MinMaxLong> {
  override fun isValid(value: MinMaxLong?, context: ConstraintValidatorContext): Boolean {
    if (value == null) {
      return true
    }
    return value.min >= 0 && value.max >= 0
  }
}

internal class PositiveOrZeroValidatorForMinMaxDouble : ConstraintValidator<PositiveOrZero, MinMaxDouble> {
  override fun isValid(value: MinMaxDouble?, context: ConstraintValidatorContext): Boolean {
    if (value == null) {
      return true
    }
    return value.min >= 0 && value.max >= 0
  }
}
