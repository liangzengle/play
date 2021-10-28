package play.res.validation

import jakarta.validation.ConstraintValidatorContext
import play.res.AbstractResource
import play.res.GroupedResourceSet

class ReferToGroupConstraintValidator : ResourceConstraintValidator<ReferToGroup, Any>() {

  private lateinit var referToResource: Class<out AbstractResource>

  override fun initialize(referTo: ReferToGroup) {
    referToResource = referTo.value.java
  }

  override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
    if (value == null || (value is Number && value.toLong() == 0L)) return true
    val resourceSet = getResourceSet(context, referToResource)
    if (resourceSet !is GroupedResourceSet<*, *>) {
      return false
    }
    @Suppress("UNCHECKED_CAST")
    resourceSet as GroupedResourceSet<Any, *>
    return resourceSet.containsGroup(value)
  }
}
