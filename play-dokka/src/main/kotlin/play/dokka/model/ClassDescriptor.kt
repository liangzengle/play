package play.dokka.model

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

/**
 *
 * @author LiangZengle
 */
data class ClassDescriptor(
  val name: String,
  val desc: String,
  val functions: List<FunctionDescriptor>,
  val properties: List<PropertyDescriptor>
) {

  fun query(function: KFunction<*>): FunctionDescriptor? {
    return functions.find { match(it, function) }
  }

  fun query(property: KProperty<*>): PropertyDescriptor? {
    return properties.find { match(it, property) }
  }

  fun query(method: Method): FunctionDescriptor? {
    return functions.find { match(it, method) }
  }

  fun query(field: Field): PropertyDescriptor? {
    return properties.find { it.name == field.name }
  }

  private fun match(descriptor: PropertyDescriptor, property: KProperty<*>): Boolean {
    return descriptor.name == property.name
  }

  private fun match(descriptor: FunctionDescriptor, function: KFunction<*>): Boolean {
    if (descriptor.name != function.name) {
      return false
    }
    if (descriptor.parameters.size != function.parameters.size) {
      return false
    }
    for (i in descriptor.parameters.indices) {
      val parameterDescriptor = descriptor.parameters[i]
      val parameter = function.parameters[i]
      if (parameter.name != parameterDescriptor.name) {
        return false
      }
      // TODO compare type?
    }
    return true
  }

  private fun match(descriptor: FunctionDescriptor, method: Method): Boolean {
    if (descriptor.name != method.name) {
      return false
    }
    val parameters = method.parameters
    if (descriptor.parameters.size != parameters.size) {
      return false
    }
    for (i in descriptor.parameters.indices) {
      val parameterDescriptor = descriptor.parameters[i]
      val parameter = parameters[i]
      if (parameter.name != parameterDescriptor.name) {
        return false
      }
      // TODO compare type?
    }
    return true
  }
}
