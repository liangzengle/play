package play.codegen

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement

internal val objectMethodNames =
  setOf("hashCode", "equals", "toString", "clone", "wait", "notify", "notifyAll", "finalize")

internal fun isObjectMethod(elem: ExecutableElement) = objectMethodNames.contains(elem.simpleName.toString())

internal fun resolveTypeVariables(type: TypeName, typeTable: Map<String, TypeName>): TypeName {
  if (type is TypeVariableName) return typeTable[type.name]
    ?: throw NoSuchElementException("TypeName for ${type.name} not exists")

  if (type is WildcardTypeName) {
    if (type.outTypes.isNotEmpty()) {
      val outType = resolveTypeVariables(type.outTypes.first(), typeTable)
      return WildcardTypeName.producerOf(outType)
    }
    if (type.inTypes.isNotEmpty()) {
      val inType = resolveTypeVariables(type.inTypes.first(), typeTable)
      return WildcardTypeName.consumerOf(inType)
    }
  }

  if (type !is ParameterizedTypeName) {
    return type
  }
  val typeArguments = type.typeArguments
  val typeArgs = arrayOfNulls<TypeName>(typeArguments.size)
  for (i in typeArgs.indices) {
    val typeArg = typeArguments[i]
    typeArgs[i] = resolveTypeVariables(typeArg, typeTable)
  }
  return type.rawType.parameterizedBy(*typeArgs.requireNoNulls())
}

internal fun replaceType(type: TypeName, target: TypeName, replacement: TypeName): TypeName {
  if (type == target) {
    return replacement
  }
  if (type is ParameterizedTypeName) {
    if (type.rawType == target) {
      return replacement
    }
    val typeArgs = type.typeArguments.map { replaceType(it, target, replacement) }
    return type.rawType.parameterizedBy(typeArgs)
  }
  if (type is WildcardTypeName) {
    if (type.outTypes.isNotEmpty() && type.outTypes.first() == target) {
      return replacement
    }
    if (type.inTypes.isNotEmpty() && type.inTypes.first() == target) {
      return replacement
    }
  }
  return type
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
internal inline fun <T> Any.unsafeCast(): T = this as T

internal fun String.capitalize() = this.replaceFirstChar { it.uppercaseChar() }

internal fun String.uncapitalize() = this.replaceFirstChar { it.lowercaseChar() }

internal fun toParamStr(parameters: List<VariableElement>): String {
  return parameters.joinToString(separator = ", ") { it.simpleName }
}

internal inline fun <reified T> qualifiedName() = T::class.qualifiedName!!

internal fun String.capitalize2() = replaceFirstChar { it.uppercaseChar() }
internal fun String.uncapitalize2() = replaceFirstChar { it.lowercaseChar() }
