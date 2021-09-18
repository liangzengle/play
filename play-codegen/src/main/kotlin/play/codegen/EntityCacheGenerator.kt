package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

@AutoService(Processor::class)
class EntityCacheGenerator : PlayAnnotationProcessor() {

  private var enableSpecializedEntityCache = false

  companion object {
    private const val ENTITY_CACHE_SPECIALIZED_OPTION_NAME = "entityCache.specialized"
  }

  override fun init0(processingEnv: ProcessingEnvironment) {
    enableSpecializedEntityCache = processingEnv.options[ENTITY_CACHE_SPECIALIZED_OPTION_NAME] == "true"
  }

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf(CacheSpec.canonicalName)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    var found = false
    for (elem in roundEnv.subtypesOf(Entity)) {
      if (!elem.isAnnotationPresent(DisableCodegen)) {
        generate(elem)
        found = true
      }
    }
    return found
  }

  private fun generate(elem: TypeElement) {
    val elemClassName = elem.asClassName()
    val idType = getIdType(elem)
    val ctor = findConstructor(elem)
    val func = ctor?.let {
      val parameters = it.parameters
      val params = parameters.map { p ->
        ParameterSpec.builder(p.simpleName.toString(), p.javaToKotlinType()).build()
      }
      val b = FunSpec.builder("getOrCreate").addParameters(params)
      if (parameters.size == 1) {
        b.addStatement(
          "return getOrCreate(%L) { %T(it) }",
          parameters.first().simpleName.toString(),
          elemClassName
        )
      } else {
        val paramList = parameters.asSequence().map { p -> p.simpleName }.joinToString(", ")
        b.addStatement(
          "return getOrCreate(%L) { %T(%L) }",
          parameters.first().simpleName.toString(),
          elemClassName,
          paramList
        )
      }
      b.build()
    }
    val className = getCacheClassName(elem)
    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotations(iocSingletonAnnotations())
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(iocInjectAnnotation())
          .addParameter("cacheManager", EntityCacheManager)
          .build()
      )

    val primitiveIdCacheType: TypeName? = if (enableSpecializedEntityCache) {
      when (idType) {
        INT -> EntityCacheInt.parameterizedBy(elemClassName)
        LONG -> EntityCacheLong.parameterizedBy(elemClassName)
        else -> null
      }
    } else null
    if (primitiveIdCacheType == null) {
      classBuilder.addSuperinterface(
        EntityCache.parameterizedBy(idType).plusParameter(elemClassName),
        CodeBlock.of("cacheManager.get(%T::class.java)", elemClassName)
      )
      classBuilder.addProperty(
        PropertySpec.builder(
          "delegatee",
          EntityCache.parameterizedBy(idType).plusParameter(elemClassName),
          KModifier.PRIVATE
        )
          .initializer(CodeBlock.of("cacheManager.get(%T::class.java)", elem))
          .build()
      )

      classBuilder.addFunction(
        FunSpec.builder("unsafeOps")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
          .addStatement(
            "return delegatee as %T",
            UnsafeEntityCacheOps.parameterizedBy(idType)
          )
          .build()
      )
    } else {
      classBuilder.addSuperinterface(
        primitiveIdCacheType,
        CodeBlock.of("cacheManager.get(%T::class.java) as %T", elem, primitiveIdCacheType)
      )
        .addAnnotation(
          AnnotationSpec.builder(Suppress::class)
            .addMember("%S", "UNCHECKED_CAST")
            .build()
        )
      classBuilder.addProperty(
        PropertySpec.builder(
          "delegatee",
          primitiveIdCacheType,
          KModifier.PRIVATE
        )
          .initializer(
            CodeBlock.of("cacheManager.get(%T::class.java) as %T", elem, primitiveIdCacheType)
          )
          .build()
      )

      classBuilder.addFunction(
        FunSpec.builder("unsafeOps")
          .addStatement(
            "return delegatee as %T",
            UnsafeEntityCacheOps.parameterizedBy(idType)
          )
          .build()
      )
    }

    classBuilder.addFunction(
      FunSpec.builder("unwrap")
        .addStatement("return delegatee")
        .build()
    )

    if (func != null) {
      classBuilder.addFunction(func)
    }
    val pkg = elementUtils.getPackageOf(elem).qualifiedName.toString()
    val file = File(generatedSourcesRoot)
    FileSpec.builder(pkg, className)
      .addType(classBuilder.build())
      .build()
      .writeTo(file)
  }

  private fun getCacheClassName(entityTypeElem: TypeElement): String {
    val entityClassName = entityTypeElem.simpleName.toString()
    return if (entityClassName.endsWith("Entity")) {
      entityClassName + "Cache"
    } else if (entityClassName.endsWith("Data")) {
      entityClassName.substring(0, entityClassName.length - 4) + "EntityCache"
    } else {
      entityClassName + "EntityCache"
    }
  }

  private val entityIntType: TypeElement by lazy(LazyThreadSafetyMode.NONE) {
    elementUtils.getTypeElement(EntityInt.canonicalName)
  }
  private val entityLongType: TypeElement by lazy(LazyThreadSafetyMode.NONE) {
    elementUtils.getTypeElement(EntityLong.canonicalName)
  }

  private fun getIdType(elem: TypeElement): TypeName {
    when {
      entityIntType.isAssignableFrom(elem) -> return INT
      entityLongType.isAssignableFrom(elem) -> return LONG
      else -> {
        var superClass = elem.superclass
        while (superClass is DeclaredType) {
          val typeArguments = superClass.typeArguments
          if (typeArguments.isEmpty()) {
            superClass = superClass.asElement().unsafeCast<TypeElement>().superclass
            continue
          }
          return typeArguments[0].asTypeName()
        }
        return TypeVariableName("ID")
      }
    }
  }

  private fun findConstructor(elem: TypeElement): ExecutableElement? {
    return elem.enclosedElements.asSequence()
      .filter { it.kind == ElementKind.CONSTRUCTOR }
      .map { it as ExecutableElement }
      .filterNot { it.modifiers.contains(Modifier.PRIVATE) }
      .sortedByDescending { it.parameters.size }
      .filter { it.parameters.isNotEmpty() }
      .firstOrNull()
  }
}
