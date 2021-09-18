package play.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import javax.lang.model.element.ExecutableElement

internal val objectMethodNames =
  setOf("hashCode", "equals", "toString", "clone", "wait", "notify", "notifyAll", "finalize")

internal fun isObjectMethod(elem: ExecutableElement) =
  !objectMethodNames.contains(elem.simpleName.toString()) && elem.parameters.isEmpty()

internal fun ExecutableElement.toBuilder(typeTable: Map<String, TypeName>): FunSpec.Builder {
  val name = simpleName.toString()
  val builder = FunSpec.builder(name)
  val returnType = resolveTypeVariables(returnType.asTypeName(), typeTable)
  builder.returns(returnType)

  for (parameter in parameters) {
    val paramName = parameter.simpleName.toString()
    val param =
      ParameterSpec.builder(paramName, resolveTypeVariables(parameter.asType().asTypeName(), typeTable)).build()
    builder.addParameter(param)
  }
  return builder
}

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
