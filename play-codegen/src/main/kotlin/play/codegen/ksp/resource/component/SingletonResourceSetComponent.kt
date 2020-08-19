package play.codegen.ksp.resource.component

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import play.codegen.DelegatingResourceSet
import play.codegen.SingletonResourceSet
import play.codegen.ksp.isStatic
import play.codegen.ksp.toParamString
import play.codegen.ksp.toTypeName2

/**
 *
 * @author LiangZengle
 */
class SingletonResourceSetComponent(private val classDeclaration: KSClassDeclaration) {

  fun apply(classBuilder: TypeSpec.Builder) {
    val resourceTypeName = classDeclaration.toClassName()
    val genericSingletonResourceSet = SingletonResourceSet.parameterizedBy(resourceTypeName)
    val genericDelegatingResourceSet = DelegatingResourceSet.parameterizedBy(resourceTypeName)
    classBuilder.addProperty(
      PropertySpec.builder("underlying", genericDelegatingResourceSet, KModifier.PRIVATE)
        .initializer("%T.getOrThrow(%T::class.java)", DelegatingResourceSet, resourceTypeName).build()
    )
    classBuilder.addFunction(
      FunSpec.builder("get").addModifiers(KModifier.PUBLIC).returns(resourceTypeName)
        .addStatement("return (underlying.getDelegatee() as %T).get()", genericSingletonResourceSet).build()
    )
    for (property in classDeclaration.getDeclaredProperties()) {
      if (!property.isPublic()) {
        continue
      }
      val propertyName = property.simpleName.asString()
      val getter = FunSpec.builder("get()").addStatement("return get().%L", propertyName).build()
      val propertySpec =
        PropertySpec.builder(propertyName, property.type.toTypeName2()).getter(getter).addAnnotation(JvmStatic::class)
          .build()
      classBuilder.addProperty(propertySpec)
    }
    for (function in classDeclaration.getDeclaredFunctions()) {
      if (!function.isPublic() || function.isConstructor() || function.isStatic()) {
        continue
      }
      val functionName = function.simpleName.asString()
      val funcBuilder = FunSpec.builder(functionName).addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class)
      for (parameter in function.parameters) {
        funcBuilder.addParameter(parameter.name!!.getShortName(), parameter.type.toTypeName2())
      }
      funcBuilder.addStatement("return get().%L(%L)", functionName, toParamString(function.parameters))
      classBuilder.addFunction(funcBuilder.build())
    }
  }
}
