package play.config.validation

import play.config.AbstractConfig
import play.config.GroupedConfigSet
import javax.validation.ConstraintValidatorContext

class ReferToGroupConstraintValidator : ConfigConstraintValidator<ReferToGroup, Any>() {

  private lateinit var referToConfig: Class<out AbstractConfig>

  override fun initialize(referTo: ReferToGroup) {
    referToConfig = referTo.value.java
  }

  override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
    if (value == null || value == 0) return true
    val configSet = getConfigSet(context, referToConfig)
    if (configSet !is GroupedConfigSet<*, *, *>) {
      return false
    }
    @Suppress("UNCHECKED_CAST")
    configSet as GroupedConfigSet<Any, *, *>
    return configSet.containsGroup(value)
  }
}
