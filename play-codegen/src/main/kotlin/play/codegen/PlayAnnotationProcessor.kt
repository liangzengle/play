package play.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.*
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

abstract class PlayAnnotationProcessor : AbstractProcessor() {

  protected val typeUtils: Types get() = processingEnv.typeUtils
  protected val elementUtils: Elements get() = processingEnv.elementUtils
  protected val filer: Filer get() = processingEnv.filer
  protected val messager: Messager get() = processingEnv.messager
  protected lateinit var generatedSourcesRoot: String

  companion object {
    const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
  }

  override fun getSupportedOptions(): Set<String> {
    return setOf(KAPT_KOTLIN_GENERATED_OPTION_NAME)
  }

  override fun getSupportedSourceVersion(): SourceVersion {
    return SourceVersion.latestSupported()
  }

  abstract override fun getSupportedAnnotationTypes(): Set<String>

  final override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    val generatedSourcesRoot = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
    if (generatedSourcesRoot.isNullOrEmpty()) {
      error("Can't find the target directory for generated Kotlin files.")
      return
    }
    this.generatedSourcesRoot = generatedSourcesRoot
    init0(processingEnv)
  }

  protected open fun init0(processingEnv: ProcessingEnvironment) {
  }

  protected fun info(obj: Any?) {
    messager.printMessage(Diagnostic.Kind.NOTE, "${this.javaClass.simpleName}>>> $obj")
  }

  protected fun warn(obj: Any?) {
    messager.printMessage(Diagnostic.Kind.WARNING, "${this.javaClass.simpleName}>>> $obj")
  }

  protected fun error(obj: Any?) {
    messager.printMessage(Diagnostic.Kind.ERROR, "${this.javaClass.simpleName}>>> $obj")
  }

  protected fun error(e: Exception) {
    val writer = StringWriter()
    e.printStackTrace(PrintWriter(writer))
    messager.printMessage(Diagnostic.Kind.ERROR, "${this.javaClass.simpleName}>>>\n $writer")
  }

  protected fun RoundEnvironment.subtypesOf(superClass: ClassName): Sequence<TypeElement> {
    val superType = typeUtils.getDeclaredType(superClass.asTypeElement())
    return rootElements.asSequence()
      .filter { it.kind == ElementKind.CLASS }
      .map { it as TypeElement }
      .filter { !it.modifiers.contains(Modifier.ABSTRACT) && superType.isAssignableFrom(it.asType()) }
  }

  protected fun KClass<*>.asTypeMirror(): TypeMirror {
    return asTypeElement().asType()
  }

  protected fun ClassName.asTypeElement(): TypeElement {
    val fqcn = this.canonicalName
    val typeElement = elementUtils.getTypeElement(fqcn)
    if (typeElement == null) {
      warn("TypeElement not found: $fqcn")
    }
    return typeElement
  }

  protected fun KClass<*>.asTypeElement(): TypeElement {
    val typeElement = elementUtils.getTypeElement(this.java.name)
    if (typeElement == null) {
      warn(this.qualifiedName)
    }
    return typeElement
  }

  protected fun Element.javaToKotlinType(): TypeName {
    return asType().javaToKotlinType()
  }

  protected fun TypeMirror.javaToKotlinType(): TypeName {
    if (this is DeclaredType && typeArguments.isNotEmpty()) {
      val rawType = toClassName().javaToKotlinType()
      val typeArgs = typeArguments.map { it.javaToKotlinType() }
      return rawType.parameterizedBy(typeArgs)
    }
    val typeName = asTypeName()
    return if (typeName is ClassName) typeName.javaToKotlinType() else typeName
  }

  private fun ClassName.javaToKotlinType(): ClassName {
    return JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.toString()))?.asSingleFqName()
      ?.asString()?.let { ClassName.bestGuess(it) } ?: this
  }

  protected fun ExecutableElement.isObjectMethod(): Boolean {
    if (this.kind != ElementKind.METHOD) {
      return false
    }
    return when (this.simpleName.toString()) {
      "hashCode", "equals", "toString", "clone", "wait", "notify", "notifyAll", "finalize" -> true
      else -> false
    }
  }

  protected fun TypeElement.getPackage(): String = elementUtils.getPackageOf(this).qualifiedName.toString()

  @Suppress("UNCHECKED_CAST")
  protected fun <T> getAnnotationValue(
    element: Element,
    annotationClass: ClassName,
    propertyName: String,
    defaultValue: T
  ): T {
    return getAnnotationValue(element, annotationClass, propertyName) ?: defaultValue
  }

  @Suppress("UNCHECKED_CAST")
  protected fun <T> getAnnotationValue(
    element: Element,
    annotationClass: ClassName,
    propertyName: String
  ): T? {
    return element
      .annotationMirrors
      .first { it.annotationType.asTypeName() == annotationClass }
      .elementValues
      .entries
      .firstOrNull { it.key.simpleName.contentEquals(propertyName) }?.value?.value as? T
  }

  protected fun AnnotatedConstruct.isAnnotationPresent(type: KClass<out Annotation>): Boolean {
    return isAnnotationPresent(type.asClassName())
  }

  protected fun AnnotatedConstruct.isAnnotationPresent(type: TypeName): Boolean {
    return annotationMirrors.any { it.annotationType.asTypeName() == type }
  }

  protected fun TypeMirror.isAssignableFrom(subType: TypeMirror): Boolean {
    return typeUtils.isAssignable(typeUtils.erasure(subType), typeUtils.erasure(this))
  }

  protected fun TypeElement.isAssignableFrom(subType: TypeElement): Boolean {
    return asType().isAssignableFrom(subType.asType())
  }

  protected fun DeclaredType.toClassName() = asTypeElement().asClassName()

  protected fun isList(typeMirror: TypeMirror): Boolean {
    return List::class.asTypeMirror().isAssignableFrom(typeMirror)
  }

  protected fun iocSingletonAnnotations(): List<AnnotationSpec> {
    return elementUtils.getTypeElement(Component.canonicalName)
      ?.let { listOf(AnnotationSpec.builder(Component).build()) }
      ?: listOf(
        AnnotationSpec.builder(Singleton).build(),
        AnnotationSpec.builder(Named).build()
      )
  }

  protected fun iocInjectAnnotation(): ClassName {
    return elementUtils.getTypeElement(Autowired.canonicalName)?.let { Autowired } ?: Inject
  }
}
