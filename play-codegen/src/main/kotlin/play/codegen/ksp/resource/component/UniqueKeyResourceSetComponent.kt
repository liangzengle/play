package play.codegen.ksp.resource.component

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import play.codegen.UniqueKey
import play.codegen.UniqueKeyResourceSet
import play.codegen.ksp.getClassDeclaration
import play.codegen.ksp.toParamString
import play.codegen.ksp.toTypeName2
import play.codegen.resolveTypeVariables

/**
 *
 * @author LiangZengle
 */
class UniqueKeyResourceSetComponent(private val classDeclaration: KSClassDeclaration, private val resolver: Resolver) {

  fun apply(classBuilder: TypeSpec.Builder) {
    val uniqueKeyClass = resolver.getClassDeclaration(UniqueKey.canonicalName)
    val uniqueKeyStarProjectedType = uniqueKeyClass.asStarProjectedType()
    val implementingUniqueKeyType =
      classDeclaration.getAllSuperTypes().find { uniqueKeyStarProjectedType.isAssignableFrom(it) } ?: return

    val ksTypeArgument = implementingUniqueKeyType.arguments[0]
    val keyTypeName = ksTypeArgument.type!!.toTypeName2()
    val resourceClass = classDeclaration.toClassName()
    val uniqueKeyResourceSetClass = resolver.getClassDeclaration(UniqueKeyResourceSet.canonicalName)
    val typeTable = mapOf("K" to keyTypeName, "T" to resourceClass)
    for (function in uniqueKeyResourceSetClass.getDeclaredFunctions()) {
      val functionName = function.simpleName.asString()
      val funcBuilder = FunSpec.builder(functionName).addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class)
      for (parameter in function.parameters) {
        val parameterType = parameter.type.toTypeName2()
        val paramType = resolveTypeVariables(parameterType, typeTable)
        funcBuilder.addParameter(ParameterSpec(parameter.name!!.getShortName(), paramType))
      }
      funcBuilder.addStatement(
        "return (underlying.getDelegatee() as %T<%T, %T>).%L(%L)",
        UniqueKeyResourceSet,
        keyTypeName,
        resourceClass,
        functionName,
        toParamString(function.parameters)
      )
      classBuilder.addFunction(funcBuilder.build())
    }
  }
}
