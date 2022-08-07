package play.codegen.ksp.controller

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import play.codegen.Controller
import play.codegen.DisableCodegen
import play.codegen.GeneratePlayerRequestMessage
import play.codegen.RequestCommander
import play.codegen.ksp.*
import play.codegen.ksp.controller.model.ControllerClassDeclaration
import play.codegen.ksp.controller.model.ControllerInvokerTypeSpec
import play.codegen.ksp.controller.model.RequestDispatcherTypeSpec
import play.codegen.ksp.controller.model.RequestMessageFileSpec

class ControllerProcessor(environment: SymbolProcessorEnvironment) : AbstractSymbolProcessor(environment) {

  private val controllerSet = mutableSetOf<ControllerClassDeclaration>()

  override fun process(): List<KSAnnotated> {
   resolver.getClassesAnnotatedWith(Controller)
      .filterNot { it.isAnnotationPresent(DisableCodegen) }
      .forEach { controllerSet.add(ControllerClassDeclaration(it))  }
    return emptyList()
  }

  override fun finish() {
    generate(resolver, controllerSet)
  }

  private fun generate(resolver: Resolver, controllerClasses: Set<ControllerClassDeclaration>) {
    val singletonAnnotations = guessIocSingletonAnnotations(resolver)
    val injectAnnotation = guessIocInjectAnnotation(resolver)
    for (controllerClass in controllerClasses) {
      val controllerInvokerTypeSpec =
        ControllerInvokerTypeSpec(controllerClass, injectAnnotation, singletonAnnotations).build()
      if (controllerClass.isAnnotationPresent(GeneratePlayerRequestMessage)) {
        val requestMessageFileSpec = RequestMessageFileSpec(controllerClass).build()
        write(requestMessageFileSpec)
      }
      write(controllerInvokerTypeSpec, controllerClass.packageName.asString())
    }
    val requestDispatcherTypeSpec =
      RequestDispatcherTypeSpec(controllerClasses, injectAnnotation, singletonAnnotations).build()
    write(requestDispatcherTypeSpec, "")
  }
}

internal fun getModuleName(controllerClass: ControllerClassDeclaration): String {
  val controllerSimpleName = controllerClass.simpleName.asString()
  return if (controllerSimpleName.endsWith("Controller")) {
    controllerSimpleName.dropLast("Controller".length)
  } else {
    controllerSimpleName
  }
}

internal fun isRequestCommander(parameter: KSValueParameter): Boolean {
  return parameter.type.classDeclaration().superTypes.any { it.classDeclaration().qualifiedName?.asString() == RequestCommander.canonicalName }
}

internal fun KSValueParameter.nameEquals(name: String): Boolean {
  return this.name?.asString() == name
}

internal fun readStmt(parameter: KSValueParameter): String {
  val reader = when (val paramClassName = parameter.type.resolve().toClassName()) {
    BOOLEAN -> "readBoolean()"
    INT -> "readInt()"
    LONG -> "readLong()"
    STRING -> "readString()"
    BYTE_ARRAY -> "getPayload()"
    INT_ARRAY -> "getIntArray()"
    LONG_ARRAY -> "getLongArray()"
    LIST -> {
      when (val type = parameter.type.toTypeName()) {
        is ParameterizedTypeName -> {
          when (val componentType = type.typeArguments[0]) {
            INT -> "getIntList()"
            LONG -> "getLongList()"
            else -> throw IllegalArgumentException("Unsupported component type: $componentType")
          }
        }

        else -> throw IllegalArgumentException("Unsupported parameter type: $type")
      }
    }

    else -> "decodePayloadAs(${paramClassName.canonicalName}::class.java)"
  }
  return "request.body.$reader"
}
