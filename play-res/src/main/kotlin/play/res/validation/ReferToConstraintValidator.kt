package play.res.validation

import play.res.AbstractResource
import javax.validation.ConstraintValidatorContext

class ReferToConstraintValidator : ConfigConstraintValidator<ReferTo, Int>() {

  private lateinit var referToResource: Class<out AbstractResource>

  override fun initialize(referTo: ReferTo) {
    referToResource = referTo.value.java
  }

  override fun isValid(value: Int?, context: ConstraintValidatorContext): Boolean {
    if (value == null || value == 0) return true
    return getResourceSet(context, referToResource)?.contains(value) ?: false
  }
}
