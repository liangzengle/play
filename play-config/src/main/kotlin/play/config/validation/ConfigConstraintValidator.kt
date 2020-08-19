package play.config.validation

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import play.config.AbstractConfig
import play.config.ConfigSet
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

abstract class ConfigConstraintValidator<A : Annotation, T> : ConstraintValidator<A, T> {

  @Suppress("UNCHECKED_CAST")
  protected fun getConfigSet(
    context: ConstraintValidatorContext,
    clazz: Class<out AbstractConfig>
  ): ConfigSet<*, *>? {
    val map = context
      .unwrap(HibernateConstraintValidatorContext::class.java)
      .getConstraintValidatorPayload(Map::class.java) as Map<Class<AbstractConfig>, ConfigSet<*, *>>
    return map[clazz]
  }
}
