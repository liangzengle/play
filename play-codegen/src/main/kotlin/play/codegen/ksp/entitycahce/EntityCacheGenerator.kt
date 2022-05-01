package play.codegen.ksp.entitycahce

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import play.codegen.*
import play.codegen.ksp.*

class EntityCacheGenerator(environment: SymbolProcessorEnvironment) : AbstractSymbolProcessor(environment) {

  companion object {
    private const val DELEGATEE = "delegatee"
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
    entityClassDeclaration: KSClassDeclaration, idClassDeclaration: KSClassDeclaration
  ): TypeSpecWithPackage {
    val entityClass = entityClassDeclaration.toClassName()
    val idClass = idClassDeclaration.toClassName()

    val singletonAnnotations = guessIocSingletonAnnotations(resolver)
    val injectAnnotation = guessIocInjectAnnotation(resolver)
    val classBuilder =
      TypeSpec.classBuilder(getCacheClassName(entityClassDeclaration)).addAnnotations(singletonAnnotations)

    val multiEntityCacheKeySpec =
      getMultiEntityCacheKeySpec(idClassDeclaration, "id") ?: getMultiEntityCacheKeySpec(entityClassDeclaration, "")

    val superInterfaceTypeName = if (multiEntityCacheKeySpec == null) {
      EntityCache.parameterizedBy(idClass, entityClass)
    } else {
      MultiEntityCache.parameterizedBy(multiEntityCacheKeySpec.keyType, idClass, entityClass)
    }

    classBuilder.primaryConstructor(
      FunSpec.constructorBuilder().addParameter(DELEGATEE, superInterfaceTypeName).build()
    )
    classBuilder.addProperty(
      PropertySpec.builder(DELEGATEE, superInterfaceTypeName, KModifier.PRIVATE).initializer(DELEGATEE).build()
    )

    classBuilder.addSuperinterface(superInterfaceTypeName, CodeBlock.of(DELEGATEE, entityClass))

    classBuilder.addFunction(
      if (multiEntityCacheKeySpec == null) {
        FunSpec.constructorBuilder()
          .addAnnotation(injectAnnotation)
          .addParameter("entityCacheManager", EntityCacheManager)
          .callThisConstructor(CodeBlock.of("entityCacheManager.get(%T::class.java)", entityClass))
          .build()
      } else {
        FunSpec.constructorBuilder()
          .addAnnotation(injectAnnotation)
          .addParameter("entityCacheManager", EntityCacheManager)
          .addParameter("entityCacheLoader", EntityCacheLoader)
          .addParameter("scheduler", Scheduler)
          .callThisConstructor(
            CodeBlock.of(
              "%T(%S, { it.%L }, entityCacheManager.get(%T::class.java), entityCacheLoader, scheduler, %T.ofMinutes(30))",
              multiEntityCacheKeySpec.cacheImplClassName,
              multiEntityCacheKeySpec.keyName,
              multiEntityCacheKeySpec.keyName,
              entityClass,
              JavaDuration
            )
          )
          .build()
      }
    )

    classBuilder.addFunction(
      FunSpec.builder("unsafeOps")
        .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
        .addStatement(
          "return %L as %T", DELEGATEE, UnsafeEntityCacheOps.parameterizedBy(idClass)
        ).build()
    )

    if (isResident(entityClassDeclaration)) {
      classBuilder.addFunction(
        FunSpec.builder("getAll")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
          .addStatement(
            "return (%L as %T).getAllCached()",
            DELEGATEE,
            EntityCacheInternalApi.parameterizedBy(entityClass)
          )
          .build()
      )
    }

    val getOrCreate = getOrCreate(entityClassDeclaration)
    if (getOrCreate != null) {
      classBuilder.addFunction(getOrCreate)
    }
    val typeSpec = classBuilder.build()

    return TypeSpecWithPackage(typeSpec, entityClass.packageName)
  }

  private fun getOrCreate(entityClassDeclaration: KSClassDeclaration): FunSpec? {
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

  private fun getMultiEntityCacheKeySpec(
    classDeclaration: KSClassDeclaration,
    prefix: String
  ): MultiEntityCacheKeySpec? {
    return classDeclaration.getAllProperties().firstOrNull {
      it.isAnnotationPresent(MultiEntityCacheKey)
    }?.let {
      val keyName = if (prefix.isEmpty()) it.simpleName.asString() else "$prefix.${it.simpleName.asString()}"
      when (val keyType = it.type.resolve().toClassName()) {
        LONG -> MultiEntityCacheKeySpec(keyName, keyType, MultiEntityCacheLong)
        INT -> MultiEntityCacheKeySpec(keyName, keyType, MultiEntityCacheInt)
        else -> null
      }
    }
  }

  private fun isResident(entityClassDeclaration: KSClassDeclaration): Boolean {
    val cacheSpec = entityClassDeclaration.getAnnotationOrNull(CacheSpec)
    val loadAllOnInit = cacheSpec?.getValue<Boolean>("loadAllOnInit") ?: false
    if (!loadAllOnInit) {
      return false
    }
    return cacheSpec?.getValue<Boolean>("neverExpire") == true
      || cacheSpec?.getValue<KSType>("expireEvaluator")?.toClassName() == NeverExpireEvaluator
  }

  private class MultiEntityCacheKeySpec(
    val keyName: String,
    val keyType: ClassName,
    val cacheImplClassName: ClassName
  )
}
