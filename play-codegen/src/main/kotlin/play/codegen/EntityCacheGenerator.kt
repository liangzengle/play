package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import play.db.Entity
import play.db.cache.CacheSpec
import play.db.cache.EntityCache
import play.db.cache.EntityCacheManager
import java.io.File
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.inject.Inject
import javax.inject.Singleton
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

@AutoService(Processor::class)
class EntityCacheGenerator : PlayAnnotationProcessor() {

  override fun getSupportedAnnotationTypes0(): Set<KClass<out Annotation>> {
    return setOf(CacheSpec::class)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    roundEnv.subtypesOf(Entity::class).forEach { elem ->
      generate(elem)
    }
    return true
  }

  private fun generate(elem: TypeElement) {
    val pkg = elementUtils.getPackageOf(elem).qualifiedName.toString()
    val className = elem.simpleName.toString() + "Cache"
    val idType = elem.typeArgsOf(Entity::class, 0)
    if (idType == null) {
      error("Can't detect ID type of ${elem.simpleName}")
      return
    }
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
          elem.asClassName()
        )
      } else {
        val paramList = parameters.asSequence().map { p -> p.simpleName }.joinToString(", ")
        b.addStatement(
          "return getOrCreate(%L){ %T(%L) }",
          parameters.first().simpleName.toString(),
          elem.asClassName(),
          paramList
        )
      }
      b.build()
    }
    val classBuilder = TypeSpec.classBuilder(className)
      .addAnnotation(Singleton::class)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addAnnotation(Inject::class)
          .addParameter("cacheManager", EntityCacheManager::class)
          .build()
      )
      .addSuperinterface(
        EntityCache::class.asClassName()
          .parameterizedBy(idType.javaToKotlinType())
          .plusParameter(elem.asClassName()),
        CodeBlock.of("cacheManager.get(%L::class.java)", elem.simpleName.toString())
      )
    if (func != null) {
      classBuilder.addFunction(func)
    }
    val file = File(generatedSourcesRoot)
    FileSpec.builder(pkg, className)
      .addType(classBuilder.build())
      .build()
      .writeTo(file)
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
