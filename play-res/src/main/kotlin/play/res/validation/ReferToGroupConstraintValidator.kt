package play.res.validation

import play.res.AbstractResource
import play.res.GroupedResourceSet
import javax.validation.ConstraintValidatorContext

class ReferToGroupConstraintValidator : ConfigConstraintValidator<ReferToGroup, Any>() {

  private lateinit var referToConfig: Class<out AbstractResource>

  override fun initialize(referTo: ReferToGroup) {
    referToConfig = referTo.value.java
  }

  override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
    if (value == null || value == 0) return true
    val configSet = getConfigSet(context, referToConfig)
    if (configSet !is GroupedResourceSet<*, *>) {
      return false
    }
    @Suppress("UNCHECKED_CAST")
    configSet as GroupedResourceSet<Any, *>
    return configSet.containsGroup(value)
  }
}
