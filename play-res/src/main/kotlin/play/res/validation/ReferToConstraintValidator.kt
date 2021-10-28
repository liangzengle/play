package play.res.validation

import jakarta.validation.ConstraintValidatorContext
import play.res.AbstractResource

class ReferToConstraintValidator : ResourceConstraintValidator<ReferTo, Int>() {

  private lateinit var referToResource: Class<out AbstractResource>

  override fun initialize(referTo: ReferTo) {
    referToResource = referTo.value.java
  }

  override fun isValid(value: Int?, context: ConstraintValidatorContext): Boolean {
    if (value == null || value == 0) return true
    return getResourceSet(context, referToResource)?.contains(value) ?: false
  }
}
