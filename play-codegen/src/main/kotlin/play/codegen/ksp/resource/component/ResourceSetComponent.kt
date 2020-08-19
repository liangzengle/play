package play.codegen.ksp.resource.component

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import play.codegen.DelegatingResourceSet
import play.codegen.ResourceSet
import play.codegen.ksp.getClassDeclaration
import play.codegen.ksp.toParamString
import play.codegen.ksp.toTypeName2
import play.codegen.resolveTypeVariables

/**
 *
 * @author LiangZengle
 */
class ResourceSetComponent(private val classDeclaration: KSClassDeclaration, private val resolver: Resolver) {

  fun apply(classBuilder: TypeSpec.Builder) {
    val resourceClass = classDeclaration.toClassName()
    val genericResourceSet = ResourceSet.parameterizedBy(resourceClass)
    val genericDelegatingResourceSet = DelegatingResourceSet.parameterizedBy(resourceClass)
    classBuilder.addProperty(
      PropertySpec
        .builder("underlying", genericDelegatingResourceSet, KModifier.PRIVATE)
        .addAnnotation(JvmStatic::class)
        .initializer(
          "%T.getOrThrow(%T::class.java)",
          DelegatingResourceSet,
          resourceClass
        )
        .build()
    )

    val unwrap = FunSpec.builder("unwrap")
      .returns(genericResourceSet)
      .addStatement("return underlying")
      .addAnnotation(JvmStatic::class)
      .build()
    classBuilder.addFunction(unwrap)

    val typeTable = mapOf("T" to resourceClass)

    for (function in resolver.getClassDeclaration(ResourceSet.canonicalName).getDeclaredFunctions()) {
      val functionName = function.simpleName.asString()
      val funcBuilder = FunSpec.builder(functionName).addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class)
      for (parameter in function.parameters) {
        val parameterType = parameter.type.toTypeName2()
        funcBuilder.addParameter(parameter.name!!.asString(), resolveTypeVariables(parameterType, typeTable))
      }
      funcBuilder.addStatement("return underlying.%L(%L)", functionName, toParamString(function.parameters))
      classBuilder.addFunction(funcBuilder.build())
    }
  }
}
