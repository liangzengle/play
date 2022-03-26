package play.codegen.ksp.enumeration

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import play.codegen.*
import play.codegen.ksp.AbstractSymbolProcessor
import play.codegen.ksp.classDeclaration
import play.codegen.ksp.enumConstants
import play.codegen.ksp.isAnnotationPresent

class EnumOpsProcessor(environment: SymbolProcessorEnvironment) : AbstractSymbolProcessor(environment) {

  private val enumClassDeclarations: MutableSet<KSClassDeclaration> = LinkedHashSet()

  override fun process(): List<KSAnnotated> {
    resolver.getSymbolsWithAnnotation(qualifiedName<EnumOps>()).filterIsInstance<KSClassDeclaration>()
      .forEach(enumClassDeclarations::add)
    return emptyList()
  }

  override fun finish() {
    for (enumClassDeclaration in enumClassDeclarations) {
      generate(enumClassDeclaration)
    }
  }

  private fun generate(enumClassDeclaration: KSClassDeclaration) {
    val objectBuilder = TypeSpec.objectBuilder(enumClassDeclaration.simpleName.asString() + 's')
    val enumClassName = enumClassDeclaration.toClassName()
    val values = PropertySpec.builder("VALUES", List::class.asClassName().parameterizedBy(enumClassName))
      .addAnnotation(JvmStatic::class).initializer("%T.values().asList()", enumClassName).build()
    val list = FunSpec.builder("values").addAnnotation(JvmStatic::class).addStatement("return VALUES").build()
    val size = FunSpec.builder("size").addAnnotation(JvmStatic::class).addStatement("return VALUES.size").build()
    val getByOrdinalOrNull =
      FunSpec.builder("getByOrdinalOrNull").addAnnotation(JvmStatic::class).addParameter("ordinal", Int::class)
        .addStatement("return if (ordinal < 0 || ordinal >= VALUES.size) null else VALUES[ordinal]").build()
    val getByOrdinalOrThrow =
      FunSpec.builder("getByOrdinal").addAnnotation(JvmStatic::class).addParameter("ordinal", Int::class)
        .addStatement("return getByOrdinalOrNull(ordinal) ?: throw IllegalArgumentException(ordinal.toString())")
        .build()
    objectBuilder.addProperty(values)
    objectBuilder.addFunction(list)
    objectBuilder.addFunction(size)
    objectBuilder.addFunction(getByOrdinalOrNull)
    objectBuilder.addFunction(getByOrdinalOrThrow)
    generateGetByName(enumClassDeclaration, objectBuilder)
    generateGetById(enumClassDeclaration, objectBuilder)
    val type = objectBuilder.build()
    FileSpec.builder(enumClassName.packageName, type.name!!).addType(type).build().writeTo(codeGenerator, false)
  }

  private fun generateGetById(enumClassDeclaration: KSClassDeclaration, objectBuilder: TypeSpec.Builder) {
    val isIdEnum =
      enumClassDeclaration.superTypes.any { it.classDeclaration().qualifiedName!!.asString() == IdEnum.canonicalName }
    if (!isIdEnum) {
      return
    }
    val idProperty =
      enumClassDeclaration.getDeclaredProperties().firstOrNull { it.isAnnotationPresent(EnumId::class.asClassName()) }
    if (idProperty == null) {
      logger.error("Enum $enumClassDeclaration should have property annotated with @EnumId")
      return
    }
    val isIdPropertyPublicFinal = idProperty.hasBackingField && idProperty.isPublic() && !idProperty.isMutable
    if (!isIdPropertyPublicFinal) {
      logger.error("$enumClassDeclaration.$idProperty is not public final, most likely missing @JvmField")
      return
    }
    val enumClassName = enumClassDeclaration.toClassName()
    val getOrNullBuilder = FunSpec.builder("getOrNull").addAnnotation(JvmStatic::class).addParameter("id", Int::class)
      .returns(enumClassName.toNullable())
    val idFieldName = idProperty.simpleName.asString()
    getOrNullBuilder.beginControlFlow("return when (id)")
    for (enumConstant in enumClassDeclaration.enumConstants()) {
      val enumConstantName = enumConstant.simpleName.asString()
      getOrNullBuilder.addStatement(
        "%T.%L.%L -> %T.%L", enumClassName, enumConstantName, idFieldName, enumClassName, enumConstantName
      )
    }
    getOrNullBuilder.addStatement("else -> null").endControlFlow()
    val getOrNull = getOrNullBuilder.build()

    val getOrThrow = FunSpec.builder("getOrThrow").addAnnotation(JvmStatic::class).addParameter("id", Int::class)
      .returns(enumClassName).addStatement("return getOrNull(id) ?: throw IllegalArgumentException(id.toString())")
      .build()
    val getOrDefault = FunSpec.builder("getOrDefault").addAnnotation(JvmStatic::class).addParameter("id", Int::class)
      .addParameter("default", enumClassName).returns(enumClassName).addStatement("return getOrNull(id) ?: default")
      .build()

    objectBuilder.addFunction(getOrNull).addFunction(getOrThrow).addFunction(getOrDefault)
  }

  private fun generateGetByName(enumClassDeclaration: KSClassDeclaration, objectBuilder: TypeSpec.Builder) {
    val enumClassName = enumClassDeclaration.toClassName()
    val getOrNullBuilder =
      FunSpec.builder("getOrNull").addAnnotation(JvmStatic::class).addParameter("name", String::class)
        .returns(enumClassName.toNullable())

    getOrNullBuilder.beginControlFlow("return when (name)")
    for (enumConstant in enumClassDeclaration.enumConstants()) {
      val enumConstantName = enumConstant.simpleName.asString()
      getOrNullBuilder.addStatement("%S -> %T.%L", enumConstantName, enumClassName, enumConstantName)
    }
    val getOrNull = getOrNullBuilder.addStatement("else -> null").endControlFlow().build()

    val getOrThrow = FunSpec.builder("getOrThrow").addAnnotation(JvmStatic::class).addParameter("name", String::class)
      .returns(enumClassName).addStatement("return getOrNull(name) ?: throw IllegalArgumentException(name)").build()
    val getOrDefault =
      FunSpec.builder("getOrDefault").addAnnotation(JvmStatic::class).addParameter("name", String::class)
        .addParameter("default", enumClassName).returns(enumClassName).addStatement("return getOrNull(name) ?: default")
        .build()

    objectBuilder.addFunction(getOrNull).addFunction(getOrThrow).addFunction(getOrDefault)
  }
}
