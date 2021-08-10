package play.res.validation

import play.res.AbstractResource
import javax.validation.ConstraintValidatorContext

class ReferToConstraintValidator : ConfigConstraintValidator<ReferTo, Int>() {

  private lateinit var referToConfig: Class<out AbstractResource>

  override fun initialize(referTo: ReferTo) {
    referToConfig = referTo.value.java
  }

  override fun isValid(value: Int?, context: ConstraintValidatorContext): Boolean {
    if (value == null || value == 0) return true
    return getConfigSet(context, referToConfig)?.contains(value) ?: false
  }
}
