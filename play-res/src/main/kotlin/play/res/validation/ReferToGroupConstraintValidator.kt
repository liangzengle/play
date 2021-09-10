package play.res.validation

import play.res.AbstractResource
import play.res.GroupedResourceSet
import javax.validation.ConstraintValidatorContext

class ReferToGroupConstraintValidator : ConfigConstraintValidator<ReferToGroup, Any>() {

  private lateinit var referToResource: Class<out AbstractResource>

  override fun initialize(referTo: ReferToGroup) {
    referToResource = referTo.value.java
  }

  override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
    if (value == null || value == 0) return true
    val resourceSet = getResourceSet(context, referToResource)
    if (resourceSet !is GroupedResourceSet<*, *>) {
      return false
    }
    @Suppress("UNCHECKED_CAST")
    resourceSet as GroupedResourceSet<Any, *>
    return resourceSet.containsGroup(value)
  }
}
