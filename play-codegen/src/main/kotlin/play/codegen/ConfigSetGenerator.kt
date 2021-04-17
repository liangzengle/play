package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import java.io.File
import java.util.*
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter
import kotlin.collections.LinkedHashSet
import kotlin.reflect.KFunction
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClassifier

@AutoService(Processor::class)
class ConfigSetGenerator : PlayAnnotationProcessor() {

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf("javax.inject.Singleton", "com.google.inject.Singleton")
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elems = roundEnv
      .subtypesOf(AbstractConfig)
      .filterNot {
        it.isAnnotationPresent(Ignore) || it.isAnnotationPresent(DisableCodegen)
      }.toList()
    if (elems.isEmpty()) {
      return false
    }
    val groupedType = Grouped.asTypeElement().asType()
    val extensionKeyType = ExtensionKey.asTypeElement().asType()
    elems.forEach { elem ->
      val simpleName = elem.simpleName.toString()
      val isSingleton = isSingleton(elem)
      val objectName = simpleName + (if (isSingleton) "Conf" else "Set")
      val objectBuilder = TypeSpec
        .objectBuilder(objectName)
        .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
      if (isSingleton) {
        implementSingleton(elem, objectBuilder)
      } else {
        implementBasics(elem, objectBuilder)
        val uniqueKeyType = findUniqueKeyType(elem)
        val hasUniqueKey = uniqueKeyType != null
        if (hasUniqueKey) {
          implementUniqueKey(elem, uniqueKeyType, objectBuilder)
        }
        if (groupedType.isAssignableFrom(elem.asType())) {
          val groupIdType = findGroupIdType(elem)
          implementGrouped(elem, groupIdType, uniqueKeyType, objectBuilder)
          if (groupIdType != null && uniqueKeyType != null) {
            implementGetByKeyFromGroup(elem, groupIdType, uniqueKeyType, objectBuilder)
          }
        }
        if (extensionKeyType.isAssignableFrom(elem.asType())) {
          implementExtension(elem, objectBuilder)
        }
      }
      val file = File(generatedSourcesRoot)
      file.mkdir()
      val pkg = processingEnv.elementUtils.getPackageOf(elem).toString()
      val fileBuilder = FileSpec.builder(pkg, simpleName + "Set")
      fileBuilder
        .addType(objectBuilder.build())
        .build().writeTo(file)
    }
    return false
  }

  private fun implementBasics(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    val genericBasicConfigSet = BasicConfigSet.parameterizedBy(elem.asClassName())
    classBuilder.addProperty(
      PropertySpec
        .builder("configSet", genericBasicConfigSet, KModifier.PRIVATE)
        .addAnnotation(JvmStatic::class)
        .initializer("%T.get(%T::class.java) as %T", DelegatedConfigSet, elem.asType(), genericBasicConfigSet)
        .build()
    )

    val asBasicConfigSet = FunSpec.builder("unwrap")
      .addStatement("return configSet")
      .addAnnotation(JvmStatic::class)
      .build()
    classBuilder.addFunction(asBasicConfigSet)

    val basicConfigSetKmClass = BasicConfigSet.asTypeElement().toImmutableKmClass()
    basicConfigSetKmClass.functions.forEach { func ->
      val funBuilder = FunSpec.builder(func.name)
      func.valueParameters.forEach { p ->
        funBuilder.addParameter(p.name, p.type.toTypeName())
      }
      val params = func.valueParameters.asSequence().map { it.name }.joinToString(", ")
      funBuilder.addModifiers(getModifiers(func))
        .addAnnotation(JvmStatic::class)
        .addStatement("return configSet.%L(%L)", func.name, params)
      classBuilder.addFunction(funBuilder.build())
    }
  }

  private fun findUniqueKeyType(elem: TypeElement): DeclaredType? {
    return elem.interfaces.asSequence()
      .map { it as DeclaredType }
      .find {
        val interfaceName = it.asElement().toString()
        interfaceName == UniqueKey.canonicalName || interfaceName == ComparableUniqueKey.canonicalName
      }?.let { it.typeArguments[0] } as? DeclaredType
  }

  private fun implementUniqueKey(elem: TypeElement, keyType: DeclaredType?, classBuilder: TypeSpec.Builder) {
    if (keyType == null) {
      error("keyType is null")
      return
    }
    val uniqueKeyConfigSetKmClass = UniqueKeyConfigSet.asTypeElement().toImmutableKmClass()
    uniqueKeyConfigSetKmClass.functions
      .forEach { func ->
        val funBuilder = FunSpec.builder(func.name)
        func.valueParameters.forEach { p ->
          val pType = p.type!!
          val parameterType = when (pType.classifier) {
            is KmClassifier.Class -> pType.toTypeName()
            is KmClassifier.TypeParameter -> keyType.javaToKotlinType()
            else -> null
          }
          if (parameterType == null) {
            error("unable to detect parameterType from ${p.type}")
            return
          }
          funBuilder.addParameter(p.name, parameterType)
        }
        val params = func.valueParameters.asSequence().map { it.name }.joinToString(", ")
        funBuilder.addModifiers(getModifiers(func))
          .addStatement(
            "return (configSet as %T<%T, %T>).%L(%L)",
            UniqueKeyConfigSet,
            keyType.javaToKotlinType(),
            elem,
            func.name,
            params
          )
          .addAnnotation(JvmStatic::class)
        classBuilder.addFunction(funBuilder.build())
      }
  }

  private fun findGroupIdType(elem: TypeElement): DeclaredType? {
    return elem.interfaces.asSequence()
      .map { it as DeclaredType }
      .find {
        it.asElement().toString() == Grouped.canonicalName
      }?.let { it.typeArguments[0] } as? DeclaredType
  }

  private fun implementGrouped(
    elem: TypeElement,
    groupIdType: DeclaredType?,
    uniqueKeyType: DeclaredType?,
    classBuilder: TypeSpec.Builder
  ) {
    if (groupIdType == null) {
      error("groupIdType is null.")
      return
    }
    GroupedConfigSet.asTypeElement().toImmutableKmClass().functions.asSequence()
      .forEach { func ->
        val funBuilder = FunSpec.builder(func.name)
        func.valueParameters.forEach { p ->
          val pType = p.type!!
          val parameterType = when (val typeName = pType.classifier) {
            is KmClassifier.Class -> pType.toTypeName()
            is KmClassifier.TypeParameter -> groupIdType.javaToKotlinType()
            else -> null
          }
          if (parameterType == null) {
            error("unable to detect parameterType from ${p.type}")
            return
          }
          funBuilder.addParameter(p.name, parameterType)
        }
        val params = func.valueParameters.asSequence().map { it.name }.joinToString(", ")
        funBuilder.addModifiers(getModifiers(func))
          .addStatement(
            "return (configSet as %T<%T, %T>).%L(%L)",
            GroupedConfigSet,
            groupIdType.javaToKotlinType(),
            elem,
            func.name,
            params
          )
          .addAnnotation(JvmStatic::class)
        classBuilder.addFunction(funBuilder.build())
      }
  }

  private fun implementGetByKeyFromGroup(
    elem: TypeElement,
    groupIdType: DeclaredType,
    uniqueKeyType: DeclaredType,
    classBuilder: TypeSpec.Builder
  ) {
    val groupIdTypeName = groupIdType.asTypeName()
    val uniqueKeyTypeName = uniqueKeyType.asTypeName()
    val getByKeyFromGroup = FunSpec.builder("getByKeyFromGroup")
      .addParameter("groupId", groupIdTypeName)
      .addParameter("key", uniqueKeyTypeName)
      .addStatement(
        "return (configSet as %T<%T, %T, %T>).getGroup(groupId).flatMap{ it.getByKey(key) }",
        GroupedConfigSet,
        groupIdTypeName,
        uniqueKeyTypeName,
        elem
      )
      .addAnnotation(JvmStatic::class)
    classBuilder.addFunction(getByKeyFromGroup.build())
  }

  private fun implementExtension(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    val extensionTypeOpt = elem.interfaces.asSequence()
      .map { it as DeclaredType }
      .find { it.asElement().toString() == ExtensionKey.canonicalName }
      ?.let { Optional.of(it.typeArguments[0]) } ?: Optional.empty()
    if (extensionTypeOpt.isEmpty) {
      error("extensionTypeOpt is empty")
      return
    }
    val extensionType = extensionTypeOpt.get()
    classBuilder.addFunction(
      FunSpec.builder("extension")
        .addModifiers(KModifier.PUBLIC)
        .returns(extensionType.asTypeName())
        .addStatement(
          "return (configSet as %T<%T, %T>).extension()",
          ExtensionConfigSet,
          extensionType,
          elem
        )
        .addAnnotation(JvmStatic::class)
        .build()
    )
  }

  private fun implementSingleton(type: TypeElement, classBuilder: TypeSpec.Builder) {
    val genericSingletonConfigSet = SingletonConfigSet.parameterizedBy(type.asClassName())
    classBuilder.addProperty(
      PropertySpec
        .builder("configSet", genericSingletonConfigSet, KModifier.PRIVATE)
        .initializer("%T.get(%T::class.java) as %T", DelegatedConfigSet, type.asType(), genericSingletonConfigSet)
        .build()
    )

    classBuilder
      .addFunction(
        FunSpec.builder("get")
          .addModifiers(KModifier.PUBLIC)
          .returns(type.asClassName())
          .addStatement("return configSet.get()")
          .build()
      )
    val getters = ElementFilter.fieldsIn(type.enclosedElements).asSequence().map {
      val name = it.simpleName.toString()
      val prefix = if (it.javaToKotlinType().toString() == "Boolean") "is" else "get"
      (prefix + name.capitalize()) to it
    }.toMap()
    ElementFilter.methodsIn(type.enclosedElements).asSequence()
      .filter {
        it.isPublic() && !it.isObjectMethod() && !it.simpleName.contentEquals("postInitialize")
      }
      .forEach { elem ->
        val funName = elem.simpleName.toString()
        if (getters.contains(funName)) {
          val p = getters[funName] ?: throw IllegalStateException("should not happen.")
          val getter = FunSpec.builder("get()")
            .addStatement("return configSet.get().%L", p.simpleName.toString())
            .build()
          val property =
            PropertySpec.builder(p.simpleName.toString(), p.asType().javaToKotlinType()).getter(getter)
              .addAnnotation(JvmStatic::class)
              .build()
          classBuilder.addProperty(property)
        } else {
          val funcBuilder = FunSpec.builder(funName).addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class)
          elem.parameters.forEach { p ->
            funcBuilder.addParameter(
              ParameterSpec.builder(
                p.simpleName.toString(),
                p.asType().javaToKotlinType()
              ).build()
            )
          }
          val params = elem.parameters.asSequence().map { p -> p.simpleName.toString() }.joinToString(", ")
          funcBuilder.addStatement("return configSet.get().%L(%L)", funName, params)
          classBuilder.addFunction(funcBuilder.build())
        }
      }
  }

  private fun isSingleton(elem: TypeElement): Boolean {
    return elem.annotationMirrors.any {
      val typeName = it.annotationType.asTypeName()
      typeName == SingletonConfig || typeName == Resource
    }
  }

  private fun getModifiers(func: KFunction<*>): LinkedList<KModifier> {
    val mods = LinkedList<KModifier>()
    if (func.isInfix) {
      mods.add(KModifier.INFIX)
    }
    if (func.isInline) {
      mods.add(KModifier.INLINE)
    }
    if (func.isOperator) {
      mods.add(KModifier.OPERATOR)
    }
//        if (func.isAbstract) {
//            mods.add(KModifier.ABSTRACT)
//        }
    if (func.isFinal) {
      mods.add(KModifier.FINAL)
    }
//        if (func.isOpen) {
//            mods.add(KModifier.OPEN)
//        }
    if (func.isSuspend) {
      mods.add(KModifier.SUSPEND)
    }
    return mods
  }

  private fun getModifiers(func: ImmutableKmFunction): Collection<KModifier> {
    val mods = LinkedHashSet<KModifier>()
    if (Flag.Function.IS_INFIX(func.flags)) {
      mods.add(KModifier.INFIX)
    }
    if (Flag.Function.IS_INLINE(func.flags)) {
      mods.add(KModifier.INLINE)
    }
    if (Flag.Function.IS_OPERATOR(func.flags)) {
      mods.add(KModifier.OPERATOR)
    }
//        if (func.isAbstract) {
//            mods.add(KModifier.ABSTRACT)
//        }
    if (Flag.IS_FINAL(func.flags)) {
      mods.add(KModifier.FINAL)
    }
//        if (func.isOpen) {
//            mods.add(KModifier.OPEN)
//        }
    if (Flag.Function.IS_SUSPEND(func.flags)) {
      mods.add(KModifier.SUSPEND)
    }
    return mods
  }
}
