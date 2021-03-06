package play.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import javax.annotation.processing.*
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

abstract class PlayAnnotationProcessor : AbstractProcessor() {

  protected lateinit var typeUtils: Types
  protected lateinit var elementUtils: Elements
  protected lateinit var filer: Filer
  protected lateinit var messager: Messager
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

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    typeUtils = processingEnv.typeUtils
    elementUtils = processingEnv.elementUtils
    filer = processingEnv.filer
    messager = processingEnv.messager
    val generatedSourcesRoot = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
    if (generatedSourcesRoot.isNullOrEmpty()) {
      error("Can't find the target directory for generated Kotlin files.")
      return
    }
    this.generatedSourcesRoot = generatedSourcesRoot
  }

  protected fun info(obj: Any?) {
    messager.printMessage(Diagnostic.Kind.NOTE, obj.toString())
  }

  protected fun warn(obj: Any?) {
    messager.printMessage(Diagnostic.Kind.WARNING, obj.toString())
  }

  protected fun error(obj: Any?) {
    messager.printMessage(Diagnostic.Kind.ERROR, obj.toString())
  }

  protected fun RoundEnvironment.subtypesOf(type: KClass<*>): Sequence<TypeElement> {
    val declaredType = type.asTypeMirror() as DeclaredType
    return subtypesOf(declaredType)
  }

  protected fun RoundEnvironment.subtypesOf(typeMirror: TypeMirror): Sequence<TypeElement> {
    val declaredType = typeMirror as DeclaredType
    val typeArgs: Array<TypeMirror> =
      declaredType.typeArguments.map { typeUtils.getWildcardType(null, null) }.toTypedArray()
    return rootElements.asSequence()
      .filter { it.kind == ElementKind.CLASS }
      .map { it as TypeElement }
      .filterNot { it.modifiers.contains(Modifier.ABSTRACT) }
      .filter {
        typeUtils.isSubtype(it.asType(), typeUtils.getDeclaredType(typeMirror.asElement() as TypeElement, *typeArgs))
      }
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
      warn(fqcn)
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

  protected fun typeMirror(canonicalName: String): TypeMirror {
    return elementUtils.getTypeElement(canonicalName).asType()
  }

  protected fun TypeElement.typeArgsOf(lookupType: KClass<*>, index: Int): TypeMirror? {
    return typeArgsOf(lookupType)?.get(index)
  }

  protected fun TypeElement.typeArgsOf(lookupType: KClass<*>): MutableList<out TypeMirror>? {
    var typeMirror: DeclaredType? = null
    var superType = this.superclass as DeclaredType
    while (true) {
      if (superType.asTypeName() == Object::class.asTypeName()) {
        typeMirror = null
        break
      } else if (superType.asElement().toString() == lookupType.qualifiedName) {
        typeMirror = superType
        break
      } else {
        val superClass = (superType.asElement() as TypeElement).superclass
        if (superClass.kind == TypeKind.NONE) {
          break
        } else {
          superType = superClass as DeclaredType
        }
      }
    }
    if (typeMirror == null) {
      return null
    }
    return typeMirror.typeArguments
  }

  protected fun TypeElement.typeArgsOf(lookupType: ClassName, index: Int): TypeMirror? {
    return typeArgsOf(lookupType)?.get(index)
  }

  protected fun TypeElement.typeArgsOf(lookupType: ClassName): MutableList<out TypeMirror>? {
    var typeMirror: DeclaredType? = null
    var superType = this.superclass as DeclaredType
    while (true) {
      if (superType.asTypeName() == Object::class.asTypeName()) {
        typeMirror = null
        break
      } else if (superType.asElement().toString() == lookupType.canonicalName) {
        typeMirror = superType
        break
      } else {
        val superClass = (superType.asElement() as TypeElement).superclass
        if (superClass.kind == TypeKind.NONE) {
          break
        } else {
          superType = superClass as DeclaredType
        }
      }
    }
    if (typeMirror == null) {
      return null
    }
    return typeMirror.typeArguments
  }

  protected fun Element.javaToKotlinType(): TypeName {
//    val className = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.asType().asTypeName().toString()))
//      ?.asSingleFqName()?.asString()
//    return className?.let { ClassName.bestGuess(className) } ?: this.asType().asTypeName()
    return asType().javaToKotlinType()
  }

  protected fun TypeMirror.javaToKotlinType(): TypeName {
//        val className =
//            JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.asTypeName().toString()))?.asSingleFqName()
//                ?.asString()
//        return className?.let { ClassName.bestGuess(className) } ?: this.asTypeName()
    return if (this is DeclaredType && this.typeArguments.isNotEmpty()) {
      val args = this.typeArguments.map { it.javaToKotlinType() }
      val rawType = (this.asTypeName() as ParameterizedTypeName).rawType
      ClassName.bestGuess(rawType.toString()).javaToKotlinType().parameterizedBy(*args.toTypedArray())
    } else {
      val className =
        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.asTypeName().toString()))?.asSingleFqName()
          ?.asString()
      className?.let { ClassName.bestGuess(className) } ?: this.asTypeName()
    }
  }

//    protected fun TypeMirror.javaToKotlinType2(): TypeName {
//        return if (this is DeclaredType && this.typeArguments.isNotEmpty()) {
//            val args = this.typeArguments.map { it.javaToKotlinType2() }
//            val rawType = (this.asTypeName() as ParameterizedTypeName).rawType
//            ClassName.bestGuess(rawType.toString()).javaToKotlinType().parameterizedBy(*args.toTypedArray())
//        } else {
//            val className =
//                JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.asTypeName().toString()))?.asSingleFqName()
//                    ?.asString()
//            className?.let { ClassName.bestGuess(className) } ?: this.asTypeName()
//        }
//    }

  private fun ClassName.javaToKotlinType(): ClassName {
    val className =
      JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.canonicalName))?.asSingleFqName()
        ?.asString()
    return className?.let { ClassName.bestGuess(className) } ?: this
  }

//    protected fun TypeMirror.javaToKotlinType(): TypeName {
//        val className =
//            JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.asTypeName().toString()))?.asSingleFqName()
//                ?.asString()
//        return className?.let { ClassName.bestGuess(className) } ?: this.asTypeName()
//    }

  protected fun Element.isPublic() = this.modifiers.contains(Modifier.PUBLIC)

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
    annotationClass: KClass<out Annotation>,
    propertyName: String,
    defaultValue: T
  ): T {
    return element
      .annotationMirrors
      .first { it.annotationType == annotationClass.asTypeMirror() }
      .elementValues
      .entries
      .firstOrNull {
        it.key.simpleName.toString() == propertyName
      }?.value?.value as? T ?: defaultValue
  }

  @Suppress("UNCHECKED_CAST")
  protected fun <T> getAnnotationValue(
    element: Element,
    annotationClass: ClassName,
    propertyName: String,
    defaultValue: T
  ): T {
    return element
      .annotationMirrors
      .first { it.annotationType.asTypeName() == annotationClass }
      .elementValues
      .entries
      .firstOrNull {
        it.key.simpleName.toString() == propertyName
      }?.value?.value as? T ?: defaultValue
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
      .firstOrNull {
        it.key.simpleName.toString() == propertyName
      }?.value?.value as? T
  }

  protected fun TypeElement.listMethods(): MutableList<ExecutableElement> {
    return ElementFilter.methodsIn(enclosedElements)
  }

  protected fun TypeElement.listVariables(): MutableList<VariableElement> {
    return ElementFilter.fieldsIn(enclosedElements)
  }

  protected fun getOption(key: String, defaultValue: String): String {
    return processingEnv.options.getOrDefault(key, defaultValue)
  }

  protected fun AnnotatedConstruct.isAnnotationPresent(type: KClass<out Annotation>): Boolean {
    return isAnnotationPresent(type.asClassName())
  }

  protected fun AnnotatedConstruct.isAnnotationPresent(type: TypeName): Boolean {
    return annotationMirrors.any { it.annotationType.asTypeName() == type }
  }

  internal fun AnnotatedConstruct.isAnnotationPresent(type: ClassName): Boolean {
    return annotationMirrors.any { it.annotationType.asTypeName() == type }
  }

  @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
  inline fun <T> Any.unsafeCast(): T = this as T

  protected fun TypeElement.isAssignableFrom(superType: ClassName): Boolean {
    return this.asType().isAssignableFrom(superType.asTypeElement().asType())
  }

  protected fun TypeMirror.isAssignableFrom(subType: TypeMirror): Boolean {
    return typeUtils.isAssignable(typeUtils.erasure(subType), typeUtils.erasure(this))
  }

  protected fun TypeElement.isAssignableFrom(subType: TypeElement): Boolean {
    return typeUtils.isAssignable(typeUtils.erasure(subType.asType()), typeUtils.erasure(asType()))
  }

  protected fun DeclaredType.toClassName() = ClassName.bestGuess(typeUtils.erasure(this).toString())

  protected fun isLong(typeMirror: TypeMirror): Boolean {
    return typeUtils.isSameType(typeMirror, Long::class.asTypeMirror())
  }

  protected fun isString(typeMirror: TypeMirror): Boolean {
    return typeUtils.isSameType(typeMirror, String::class.asTypeMirror())
  }

  protected fun isList(typeMirror: TypeMirror): Boolean {
    return List::class.asTypeMirror().isAssignableFrom(typeMirror)
  }
}
