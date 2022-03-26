package play.codegen.ksp.resource.component

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import play.codegen.*
import play.codegen.ksp.*

/**
 *
 * @author LiangZengle
 */
class GroupedResourceSetComponent(private val classDeclaration: KSClassDeclaration, private val resolver: Resolver) {

  fun apply(classBuilder: TypeSpec.Builder) {
    val groupIdType: TypeName
    val uniqueKeyType: TypeName?
    if (resolver.isAssignable(GroupedWithUniqueKey.canonicalName, classDeclaration)) {
      groupIdType = classDeclaration.getTypeArg(resolver, GroupedWithUniqueKey.canonicalName, 0)
      uniqueKeyType = classDeclaration.getTypeArg(resolver, GroupedWithUniqueKey.canonicalName, 1)
    } else {
      groupIdType = classDeclaration.getTypeArg(resolver, Grouped.canonicalName, 0)
      uniqueKeyType = null
    }
    val resourceClass = classDeclaration.toClassName()
    var parameterizedUniqueKeyResourceGroup: TypeName? = null
    if (uniqueKeyType != null) {
      parameterizedUniqueKeyResourceGroup = UniqueKeyResourceGroup.parameterizedBy(resourceClass, uniqueKeyType)
    }

    val typeTable = mapOf("G" to groupIdType, "T" to resourceClass)

    val parameterizeResourceGroup = ResourceGroup.parameterizedBy(resourceClass)
    val groupedResourceSetDeclaration = resolver.getClassDeclaration(GroupedResourceSet.canonicalName)
    for (func in groupedResourceSetDeclaration.getDeclaredFunctions()) {
      val functionName = func.simpleName.asString()
      val originalReturnType =
        func.returnType?.let { resolveTypeVariables(it.toTypeName2(), typeTable) } ?: UNIT
      val returnType = if (parameterizedUniqueKeyResourceGroup != null) {
        replaceType(originalReturnType, parameterizeResourceGroup, parameterizedUniqueKeyResourceGroup)
      } else originalReturnType
      val funcBuilder =
        FunSpec.builder(functionName).addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class).returns(returnType)
      for (parameter in func.parameters) {
        val paramType = resolveTypeVariables(parameter.type.toTypeName2(), typeTable)
        funcBuilder.addParameter(parameter.name!!.asString(), paramType)
      }
      if (originalReturnType != returnType) {
        funcBuilder.addStatement(
          "return (underlying.getDelegatee() as %T).%L(%L) as %T",
          GroupedResourceSet.parameterizedBy(groupIdType, resourceClass),
          functionName,
          toParamString(func.parameters),
          returnType
        )
      } else {
        funcBuilder.addStatement(
          "return (underlying.getDelegatee() as %T).%L(%L)",
          GroupedResourceSet.parameterizedBy(groupIdType, resourceClass),
          functionName,
          toParamString(func.parameters)
        )
      }
      classBuilder.addFunction(funcBuilder.build())
    }
  }
}
