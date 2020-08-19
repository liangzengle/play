package play.codegen.ksp.entitycahce

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import play.codegen.*
import play.codegen.ksp.*

class EntityCacheGenerator(environment: SymbolProcessorEnvironment) : AbstractSymbolProcessor(environment) {

  companion object {
    private const val ENTITY_CACHE_SPECIALIZED_OPTION_NAME = "entityCache.specialized"
  }

  private val entityCacheTypeSpecSet = hashSetOf<TypeSpecWithPackage>()

  override fun process(): List<KSAnnotated> {
    val subclasses = resolver.getAllSubclasses(Entity.canonicalName).toSet()
    for (entityClass in subclasses) {
      if (!entityClass.isAnnotationPresent(DisableCodegen)) {
        val idClass = getIdType(entityClass)
        val typeSpec = generate(entityClass, idClass)
        entityCacheTypeSpecSet.add(typeSpec)
      }
    }
    return emptyList()
  }

  override fun finish() {
    for (o in entityCacheTypeSpecSet) {
      write(o)
    }
  }

  private fun getIdType(entityClassDeclaration: KSClassDeclaration): KSClassDeclaration {
    if (resolver.isAssignable(resolver.getClassDeclaration(EntityLong.canonicalName), entityClassDeclaration)) {
      return resolver.builtIns.longType.declaration as KSClassDeclaration
    }
    if (resolver.isAssignable(resolver.getClassDeclaration(EntityInt.canonicalName), entityClassDeclaration)) {
      return resolver.builtIns.intType.declaration as KSClassDeclaration
    }
    return entityClassDeclaration.getAllProperties()
      .first { it.simpleName.getShortName() == "id" }.type.classDeclaration()
  }

  private fun generate(
    entityClassDeclaration: KSClassDeclaration,
    idClassDeclaration: KSClassDeclaration
  ): TypeSpecWithPackage {
    val entityClass = entityClassDeclaration.toClassName()
    val idClass = idClassDeclaration.toClassName()

    val singletonAnnotations = guessIocSingletonAnnotations(resolver)
    val injectAnnotation = guessIocInjectAnnotation(resolver)
    val classBuilder =
      TypeSpec.classBuilder(getCacheClassName(entityClassDeclaration)).addAnnotations(singletonAnnotations)
        .primaryConstructor(
          FunSpec.constructorBuilder().addAnnotation(injectAnnotation).addParameter("cacheManager", EntityCacheManager)
            .build()
        )
    val enableSpecializedEntityCache = options[ENTITY_CACHE_SPECIALIZED_OPTION_NAME] == "true"
    val primitiveEntityCacheType = if (enableSpecializedEntityCache) {
      when (idClassDeclaration) {
        resolver.builtIns.intType -> EntityCacheInt.parameterizedBy(entityClass)
        resolver.builtIns.longType -> EntityCacheLong.parameterizedBy(entityClass)
        else -> null
      }
    } else null

    if (primitiveEntityCacheType == null) {
      classBuilder.addSuperinterface(
        EntityCache.parameterizedBy(idClass).plusParameter(entityClass),
        CodeBlock.of("cacheManager.get(%T::class.java)", entityClass)
      )
      classBuilder.addProperty(
        PropertySpec.builder(
          "delegatee", EntityCache.parameterizedBy(idClass).plusParameter(entityClass), KModifier.PRIVATE
        ).initializer(CodeBlock.of("cacheManager.get(%T::class.java)", entityClass)).build()
      )

      classBuilder.addFunction(
        FunSpec.builder("unsafeOps")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
          .addStatement(
            "return delegatee as %T", UnsafeEntityCacheOps.parameterizedBy(idClass)
          ).build()
      )
    } else {
      classBuilder.addSuperinterface(
        primitiveEntityCacheType,
        CodeBlock.of("cacheManager.get(%T::class.java) as %T", entityClass, primitiveEntityCacheType)
      ).addAnnotation(
        AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build()
      )
      classBuilder.addProperty(
        PropertySpec.builder(
          "delegatee", primitiveEntityCacheType, KModifier.PRIVATE
        ).initializer(
          CodeBlock.of("cacheManager.get(%T::class.java) as %T", entityClass, primitiveEntityCacheType)
        ).build()
      )

      classBuilder.addFunction(
        FunSpec.builder("unsafeOps").addStatement(
          "return delegatee as %T", UnsafeEntityCacheOps.parameterizedBy(idClass)
        ).build()
      )
    }
    classBuilder.addFunction(
      FunSpec.builder("unwrap").addStatement("return delegatee").build()
    )

    val ctor = buildConstructor(entityClassDeclaration)
    if (ctor != null) {
      classBuilder.addFunction(ctor)
    }
    val typeSpec = classBuilder.build()

    return TypeSpecWithPackage(typeSpec, entityClass.packageName)
  }

  private fun buildConstructor(entityClassDeclaration: KSClassDeclaration): FunSpec? {
    val entityClass = entityClassDeclaration.toClassName()
    val constructor = entityClassDeclaration.primaryConstructor ?: return null
    val parameters = constructor.parameters
    return if (parameters.size == 1) {
      val p = parameters[0]
      val params = listOf(toParameterSpec(p))
      FunSpec.builder("getOrCreate").addParameters(params).addStatement(
        "return getOrCreate(%L) { %T(it) }", p.name!!.getShortName(), entityClass
      ).build()
    } else if (parameters.size > 1) {
      val params = parameters.map { p -> toParameterSpec(p) }
      val paramList = parameters.asSequence().map { p -> p.name!!.asString() }.joinToString(", ")
      FunSpec.builder("getOrCreate").addParameters(params).addStatement(
        "return getOrCreate(%L) { %T(%L) }", parameters.first().name!!.getShortName(), entityClass, paramList
      ).build()
    } else {
      logger.error("${entityClassDeclaration}'s primary constructor should have at least one parameter")
      throw IllegalStateException()
    }
  }

  private fun getCacheClassName(entityClassDeclaration: KSClassDeclaration): String {
    val simpleName = entityClassDeclaration.simpleName.getShortName()
    return if (simpleName.endsWith("Entity")) {
      simpleName + "Cache"
    } else if (simpleName.endsWith("Data")) {
      simpleName.substring(0, simpleName.length - 4) + "EntityCache"
    } else {
      simpleName + "EntityCache"
    }
  }
}
