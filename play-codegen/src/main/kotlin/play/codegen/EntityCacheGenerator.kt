package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

@AutoService(Processor::class)
class EntityCacheGenerator : PlayAnnotationProcessor() {

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf(CacheSpec.canonicalName)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    return roundEnv.subtypesOf(Entity)
      .filterNot { it.isAnnotationPresent(DisableCodegen) }
      .onEach { elem ->
        generate(elem)
      }.count() != 0
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
    val className = elem.simpleName.toString() + "Cache"
    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotation(Singleton)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(Inject)
          .addParameter("cacheManager", EntityCacheManager)
          .build()
      )
      .addSuperinterface(
        EntityCache.parameterizedBy(idType.asTypeName()).plusParameter(elemClassName),
        CodeBlock.of("cacheManager.get(%L::class.java)", elem.simpleName.toString())
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

  private val entityIntType: TypeElement by lazy(LazyThreadSafetyMode.NONE) {
    elementUtils.getTypeElement(EntityInt.canonicalName)
  }
  private val entityLongType: TypeElement by lazy(LazyThreadSafetyMode.NONE) {
    elementUtils.getTypeElement(EntityLong.canonicalName)
  }
  private val entityStringType: TypeElement by lazy(LazyThreadSafetyMode.NONE) {
    elementUtils.getTypeElement(EntityString.canonicalName)
  }

  private fun getIdType(elem: TypeElement): KClass<*> {
    return when {
      entityIntType.isAssignableFrom(elem) -> Int::class
      entityLongType.isAssignableFrom(elem) -> Long::class
      entityStringType.isAssignableFrom(elem) -> String::class
      else -> throw IllegalStateException("Can't detect ID type of ${elem.simpleName}")
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
