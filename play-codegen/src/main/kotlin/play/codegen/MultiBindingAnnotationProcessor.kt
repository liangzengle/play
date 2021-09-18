package play.codegen

import com.google.auto.service.AutoService
import com.google.common.collect.Sets
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import java.io.IOException
import java.util.*
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.tools.StandardLocation

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

  protected fun appendServices(providerInterface: String, services: Collection<String>) {
    val resourceFile = "META-INF/services/$providerInterface"
    info("Working on resource file: $resourceFile")
    try {
      val allServices: SortedSet<String> = Sets.newTreeSet()
      try {
        // would like to be able to print the full path
        // before we attempt to get the resource in case the behavior
        // of filer.getResource does change to match the spec, but there's
        // no good way to resolve CLASS_OUTPUT without first getting a resource.
        val existingFile = filer.getResource(
          StandardLocation.CLASS_OUTPUT, "",
          resourceFile
        )
        info("Looking for existing resource file at " + existingFile.toUri())
        val oldServices = existingFile.openInputStream().bufferedReader()
          .lineSequence().toCollection(linkedSetOf())
        info("Existing service entries: $oldServices")
        allServices.addAll(oldServices)
      } catch (e: IOException) {
        // According to the javadoc, Filer.getResource throws an exception
        // if the file doesn't already exist.  In practice this doesn't
        // appear to be the case.  Filer.getResource will happily return a
        // FileObject that refers to a non-existent file but will throw
        // IOException if you try to open an input stream for it.
        info("Resource file did not already exist.")
      }
      val newServices = HashSet(services)
      if (allServices.containsAll(newServices)) {
        info("No new service entries being added.")
        return
      }
      allServices.addAll(newServices)
      info("New service file contents: $allServices")
      val fileObject = filer.createResource(
        StandardLocation.CLASS_OUTPUT, "",
        resourceFile
      )
      val out = fileObject.openOutputStream()
      out.bufferedWriter().use { write ->
        allServices.forEach {
          write.write(it)
          write.newLine()
        }
      }
      info("Wrote to: " + fileObject.toUri())
    } catch (e: IOException) {
      error("Unable to create $resourceFile, $e")
      return
    }
  }
}
