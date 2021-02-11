package play

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * Annotation Processor for [[EagerlyLoad]]
 * @author LiangZengle
 */
@AutoService(Processor::class)
internal class EagerlyLoadProcessor : AbstractProcessor() {

  companion object {
    const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
  }

  private lateinit var generatedSourcesRoot: String

  private val eagerlyLoadClasses = mutableSetOf<String>()

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    val generatedSourcesRoot = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
    if (generatedSourcesRoot.isNullOrEmpty()) {
      error("Can't find the target directory for generated Kotlin files.")
    } else {
      this.generatedSourcesRoot = generatedSourcesRoot
    }
  }

  override fun getSupportedOptions(): Set<String> {
    return setOf(KAPT_KOTLIN_GENERATED_OPTION_NAME)
  }

  override fun getSupportedSourceVersion(): SourceVersion {
    return SourceVersion.latestSupported()
  }

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf(EagerlyLoad::class.qualifiedName!!)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    return try {
      processImpl(roundEnv)
    } catch (e: Exception) {
      val writer = StringWriter()
      e.printStackTrace(PrintWriter(writer))
      fatalError(writer.toString())
      true
    }
  }

  private fun processImpl(roundEnv: RoundEnvironment): Boolean {
    if (roundEnv.processingOver()) {
      generateLoader()
    } else {
      processAnnotation(roundEnv)
    }
    return true
  }

  private fun generateLoader() {
    val load = FunSpec.builder("load")
    for (bootstrapClass in eagerlyLoadClasses) {
      load.addStatement("Class.forName(%S)", bootstrapClass)
    }
    val obj = TypeSpec.objectBuilder("EagerlyLoader")
      .addFunction(load.build())
      .build()

    val dir = File(generatedSourcesRoot)
    dir.mkdirs()
    val fileBuilder = FileSpec.builder("", obj.name!!)
    fileBuilder
      .addType(obj)
      .build()
      .writeTo(dir)
  }

  private fun processAnnotation(roundEnv: RoundEnvironment) {
    val elements = roundEnv.getElementsAnnotatedWith(EagerlyLoad::class.java)
    for (e in elements) {
      val elem = e as TypeElement
      val fqcn = elem.qualifiedName.toString()
      eagerlyLoadClasses += fqcn

      log("Add BootstrapClass: $fqcn")
    }
  }

  private fun log(msg: String) {
    if (processingEnv.options.containsKey("debug")) {
      processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, msg)
    }
  }

  private fun error(msg: String, element: Element, annotation: AnnotationMirror) {
    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, msg, element, annotation)
  }

  private fun fatalError(msg: String) {
    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: $msg")
  }
}
