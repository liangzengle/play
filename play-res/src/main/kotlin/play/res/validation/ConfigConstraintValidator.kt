package play.res.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import play.res.AbstractResource
import play.res.ResourceSet

abstract class ConfigConstraintValidator<A : Annotation, T> : ConstraintValidator<A, T> {

  @Suppress("UNCHECKED_CAST")
  protected fun getResourceSet(
    context: ConstraintValidatorContext,
    clazz: Class<out AbstractResource>
  ): ResourceSet<*>? {
    val map = context
      .unwrap(HibernateConstraintValidatorContext::class.java)
      .getConstraintValidatorPayload(Map::class.java) as Map<Class<AbstractResource>, ResourceSet<*>>
    return map[clazz]
  }
}
