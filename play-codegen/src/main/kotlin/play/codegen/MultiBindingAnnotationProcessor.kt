package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import java.util.*
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

/**
 *
 * @author LiangZengle
 */
@AutoService(Processor::class)
class MultiBindingAnnotationProcessor : PlayAnnotationProcessor() {
  override fun getSupportedAnnotationTypes(): Set<String> {
    return Collections.singleton(EnableMultiBinding.canonicalName)
  }

  private val guiceModules = LinkedList<String>()

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    return try {
      processImpl(roundEnv)
    } catch (e: Exception) {
      error(e)
      true
    }
  }

  private fun processImpl(roundEnv: RoundEnvironment): Boolean {
    if (roundEnv.processingOver()) {
      writeConfigFile()
      return true
    }
    val elements = roundEnv.getElementsAnnotatedWith(EnableMultiBinding.asTypeElement())
    for (element in elements) {
      val typeElement = element as TypeElement
      try {
        process(typeElement)
      } catch (e: Exception) {
        error(e)
      }
    }
    return true
  }

  private fun process(element: TypeElement) {
    val className = "${element.simpleName}MultiBindModule"
    val typeName = if (element.typeParameters.isNotEmpty()) {
      val typeParameters = element.typeParameters.map { it.bounds[0].asTypeName() }.toList()
      element.asClassName().parameterizedBy(typeParameters)
    } else {
      element.asClassName()
    }
    val wildcardTypeName = typeName.copy(annotations = listOf(AnnotationSpec.builder(JvmWildcard::class).build()))
    val code = CodeBlock.builder()
      .addStatement(
        "bind(typeLiteral<%T>()).toProvider(%T(%M<%T>()))",
        List::class.asClassName().parameterizedBy(wildcardTypeName),
        MultiBindListProvider,
        classOf,
        typeName
      )
      .addStatement(
        "bind(typeLiteral<%T>()).toProvider(%T(%M<%T>()))",
        Set::class.asClassName().parameterizedBy(wildcardTypeName),
        MultiBindSetProvider,
        classOf,
        typeName
      )
      .build()
    val configure = FunSpec.builder("configure")
      .addModifiers(KModifier.OVERRIDE)
      .addCode(code)
      .build()

    val typeSpec = TypeSpec.classBuilder(className)
      .superclass(GeneratedMultiBindModule)
      .addFunction(configure)
      .build()
    val file = File(generatedSourcesRoot)
    val pkg = element.getPackage()
    FileSpec.builder(pkg, className)
      .addType(typeSpec)
      .build()
      .writeTo(file)
    guiceModules.add("$pkg.$className")
  }

  private fun writeConfigFile() {
    appendServices(GeneratedMultiBindModule.canonicalName, guiceModules)
  }
}
