package play.codegen.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import play.codegen.*
import kotlin.reflect.KClass

fun Resolver.getClassDeclarations(): Set<KSClassDeclaration> {
  val classDeclarations = hashSetOf<KSClassDeclaration>()
  val visitor = object : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      classDeclarations.add(classDeclaration)
    }

    override fun visitFile(file: KSFile, data: Unit) {
      file.declarations.forEach { it.accept(this, data) }
    }
  }
  for (ksFile in getAllFiles()) {
    ksFile.accept(visitor, Unit)
  }
  return classDeclarations
}

val InstantiableClassFilter: (KSClassDeclaration) -> Boolean =
  { it.isClass() && !it.isAbstract() && !it.isLocal() && !it.modifiers.contains(Modifier.INNER) }

fun Resolver.getAllSubclasses(
  superClassName: String,
  filter: (KSClassDeclaration) -> Boolean = InstantiableClassFilter
): Sequence<KSClassDeclaration> {
  val superClassDeclaration = getClassDeclarationOrNull(superClassName) ?: return emptySequence()
  val superKsType = superClassDeclaration.asStarProjectedType()
  return getClassDeclarations()
    .asSequence()
    .filter(filter)
    .filter { superKsType.isAssignableFrom(it.asStarProjectedType()) }
}

fun Resolver.getClassDeclaration(className: String): KSClassDeclaration {
  return getClassDeclarationOrNull(className) ?: throw KsClassDeclarationNotFoundException(className)
}

fun Resolver.getClassDeclarationOrNull(className: String): KSClassDeclaration? {
  return getClassDeclarationByName(getKSNameFromString(className))
}

fun Resolver.isAssignable(superType: KSClassDeclaration, subtype: KSClassDeclaration): Boolean {
  return superType.asStarProjectedType().isAssignableFrom(subtype.asStarProjectedType())
}

fun Resolver.isAssignable(superTypeName: String, subtype: KSClassDeclaration): Boolean {
  return isAssignable(getClassDeclaration(superTypeName), subtype)
}

fun KSClassDeclaration.isInterface() = classKind == ClassKind.INTERFACE

fun KSClassDeclaration.isAnnotation() = classKind == ClassKind.ANNOTATION_CLASS

fun KSClassDeclaration.isClass() = classKind == ClassKind.CLASS

fun KSClassDeclaration.getTypeArg(superKSType: KSType, index: Int): KSTypeArgument {
  for (type in getAllSuperTypes()) {
    if (superKSType.starProjection() == type.starProjection()) {
      return type.arguments[index]
    }
  }
  throw IllegalStateException("failed to get type arg: $this $superKSType $index")
}

fun Resolver.getClassesAnnotatedWith(annotationType: ClassName): Sequence<KSClassDeclaration> {
  return this.getSymbolsWithAnnotation(annotationType.canonicalName).filterIsInstance<KSClassDeclaration>()
}

fun KSClassDeclaration.enumConstants(): Sequence<KSClassDeclaration> {
  return declarations.filterIsInstance<KSClassDeclaration>().filter { it.classKind == ClassKind.ENUM_ENTRY }
}

fun KSAnnotated.getAnnotation(annotationType: KClass<out Annotation>): KSAnnotation {
  return annotations.filter {
    it.shortName.getShortName() == annotationType.simpleName && it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationType.qualifiedName
  }.first()
}

fun KSAnnotated.getAnnotation(className: ClassName): KSAnnotation {
  return annotations.filter {
    it.shortName.getShortName() == className.simpleName && it.annotationType.resolve().declaration.qualifiedName?.asString() == className.canonicalName
  }.first()
}

fun KSAnnotated.getAnnotationOrNull(className: ClassName): KSAnnotation? {
  return annotations.filter {
    it.shortName.getShortName() == className.simpleName && it.annotationType.resolve().declaration.qualifiedName?.asString() == className.canonicalName
  }.firstOrNull()
}

fun KSAnnotated.getAnnotationsByType(className: ClassName): Sequence<KSAnnotation> {
  return this.annotations.filter {
    it.shortName.getShortName() == className.simpleName && it.annotationType.resolve().declaration
      .qualifiedName?.asString() == className.canonicalName
  }
}

@Suppress("UNCHECKED_CAST")
fun <T> KSAnnotation.getValue(name: String): T {
  for (argument in arguments) {
    if (argument.name?.getShortName() == name) {
      return argument.value as T
    }
  }
  throw IllegalStateException("Annotation arg not found: $name")
}

fun KSAnnotated.isAnnotationPresent(annotationType: ClassName): Boolean {
  return annotations.any {
    it.shortName.getShortName() == annotationType.simpleName
      && it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationType.canonicalName
  }
}

fun KSTypeReference.classDeclaration() = this.resolve().declaration as KSClassDeclaration

fun KSTypeReference.toTypeName2(): TypeName {
  return toTypeName(DefaultTypeParameterResolver)
}

fun toParameterSpec(p: KSValueParameter): ParameterSpec {
  return ParameterSpec.builder(p.name!!.getShortName(), p.type.toTypeName()).build()
}

fun guessIocSingletonAnnotations(resolver: Resolver): List<AnnotationSpec> {
  val component = resolver.getClassDeclarationOrNull(Component.canonicalName)
  if (component != null) {
    return listOf(AnnotationSpec.builder(Component).build())
  }
  val jakartaSingleton = resolver.getClassDeclarationOrNull(JakartaSingleton.canonicalName)
  if (jakartaSingleton != null) {
    return listOf(AnnotationSpec.builder(JakartaSingleton).build(), AnnotationSpec.builder(JakartaNamed).build())
  }
  val javaxSingleton = resolver.getClassDeclarationOrNull(Singleton.canonicalName)
  if (javaxSingleton != null) {
    return listOf(AnnotationSpec.builder(Singleton).build(), AnnotationSpec.builder(Named).build())
  }
  return emptyList()
}

fun guessIocInjectAnnotation(resolver: Resolver): AnnotationSpec? {
  val autowired = resolver.getClassDeclarationOrNull(Autowired.canonicalName)
  if (autowired != null) {
    return AnnotationSpec.builder(Autowired).build()
  }
  val jakartaInject = resolver.getClassDeclarationOrNull(JakartaInject.canonicalName)
  if (jakartaInject != null) {
    return AnnotationSpec.builder(JakartaInject).build()
  }
  val javaxInject = resolver.getClassDeclarationOrNull(Inject.canonicalName)
  if (javaxInject != null) {
    return AnnotationSpec.builder(Inject).build()
  }
  return null
}

fun KSDeclaration.isStatic() = modifiers.contains(Modifier.JAVA_STATIC)

fun toParamString(parameters: List<KSValueParameter>): String {
  return parameters.asSequence().mapNotNull { it.name?.asString() }.joinToString(", ")
}

fun KSName?.contentEquals(content: String): Boolean {
  return this?.asString() == content
}

fun KSClassDeclaration.getTypeArg(superClassName: String, index: Int): TypeName {
  val superType =
    getAllSuperTypes()
      .firstOrNull { it.toClassName().canonicalName == superClassName }
  if (superType == null) {
    val superTypeNameList = getAllSuperTypes().map { it.toTypeName() }
    throw IllegalStateException("failed to find super type: $this $superClassName $superTypeNameList")
  }
  if (superType.arguments.size > index) {
    return superType.arguments[index].type!!.toTypeName()
  } else {
    throw IllegalStateException("failed to get type arg: $this $superType ${superType.toTypeName()} $index")
  }
}

fun KSValueParameter.toParameterSpec(): ParameterSpec.Builder {
  return ParameterSpec.builder(name!!.asString(), type.toTypeName2())
}

val DefaultTypeParameterResolver = object : TypeParameterResolver {
  override val parametersMap: Map<String, TypeVariableName> = emptyMap()

  override fun get(index: String): TypeVariableName {
    return TypeVariableName(index)
  }
}
