package play.res

import jakarta.validation.Validation
import org.hibernate.validator.HibernateValidatorFactory
import java.util.*

internal class ConstraintsValidator(
  private val resourceSets: Map<Class<AbstractResource>, ResourceSet<AbstractResource>>
) {

  private val validatorFactory = Validation.byDefaultProvider()
    .configure()
    .buildValidatorFactory()
    .unwrap(HibernateValidatorFactory::class.java)

  private val validator = validatorFactory
    .usingContext()
    .constraintValidatorPayload(resourceSets)
    .validator

  fun validateAll(): List<String> = validate(resourceSets)

  fun validateAllThrows() {
    val errors = validate(resourceSets)
    if (errors.isNotEmpty()) {
      throw InvalidResourceException(errors.joinToString("\n", "\n", ""))
    }
  }

  fun validateThrows(classes: Iterable<Class<*>>) {
    val errors = validate(resourceSets.filterKeys { classes.contains(it) })
    if (errors.isNotEmpty()) {
      throw InvalidResourceException(errors.joinToString("\n", "\n", ""))
    }
  }

  fun validate(classes: Iterable<Class<*>>): List<String> {
    return validate(resourceSets.filterKeys { classes.contains(it) })
  }

  private fun validate(resourceSets: Map<Class<AbstractResource>, ResourceSet<AbstractResource>>): List<String> {
    val errors = LinkedList<String>()
    for ((resourceClass, resourceSet) in resourceSets) {
      for (resource in resourceSet.list()) {
        val violations = validator.validate(resource)
        for (violation in violations) {
          val value = violation.constraintDescriptor.attributes["value"]
          val resourceClassName = if (value is Class<*>) value.simpleName else ""
          val msg =
            "$resourceClassName${violation.message}: ${violation.rootBean.javaClass.simpleName}(${violation.rootBean.id}).${violation.propertyPath} = ${violation.invalidValue}"
          errors.add(msg)
        }
      }
    }
    return errors
  }
}
