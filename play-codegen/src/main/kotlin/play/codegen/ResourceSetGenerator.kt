package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import kotlinx.metadata.Flag
import java.io.File
import java.util.*
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter
import kotlin.reflect.KFunction

@AutoService(Processor::class)
class ResourceSetGenerator : PlayAnnotationProcessor() {

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf("play.res.ResourcePath")
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elems = roundEnv
      .subtypesOf(AbstractResource)
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
        implementResourceSet(elem, objectBuilder)
        val uniqueKeyType = findUniqueKeyType(elem)
        val hasUniqueKey = uniqueKeyType != null
        if (hasUniqueKey) {
          implementUniqueKey(elem, uniqueKeyType, objectBuilder)
        }
        val isGrouped = groupedType.isAssignableFrom(elem.asType())
        if (isGrouped) {
          val groupIdType = findGroupIdType(elem)
          val groupUniqueKeyType = findGroupUniqueKeyType(elem)
          implementGrouped(elem, groupIdType, groupUniqueKeyType, objectBuilder)
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

  private fun implementResourceSet(
    elem: TypeElement,
    classBuilder: TypeSpec.Builder
  ) {
    val genericResourceSet = ResourceSet.parameterizedBy(elem.asClassName())
    val genericDelegatedResourceSet = DelegatedResourceSet.parameterizedBy(elem.asClassName())
    classBuilder.addProperty(
      PropertySpec
        .builder("underlying", genericDelegatedResourceSet, KModifier.PRIVATE)
        .addAnnotation(JvmStatic::class)
        .initializer(
          "%T.getOrThrow(%T::class.java)",
          DelegatedResourceSet,
          elem.asType()
        )
        .build()
    )

    val unwrap = FunSpec.builder("unwrap")
      .returns(genericResourceSet)
      .addStatement("return underlying")
      .addAnnotation(JvmStatic::class)
      .build()
    classBuilder.addFunction(unwrap)

    val resourceSetKmClass = ResourceSet.asTypeElement().toImmutableKmClass()
    resourceSetKmClass.functions.forEach { func ->
      val funBuilder = FunSpec.builder(func.name)
      func.valueParameters.forEach { p ->
        funBuilder.addParameter(p.name, p.typeName() ?: elem.asClassName())
      }
      val params = func.valueParameters.asSequence().map { it.name }.joinToString(", ")
      funBuilder.addModifiers(getModifiers(func))
        .addAnnotation(JvmStatic::class)
        .addStatement("return underlying.%L(%L)", func.name, params)
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

  private fun implementUniqueKey(
    elem: TypeElement,
    keyType: DeclaredType?,
    classBuilder: TypeSpec.Builder
  ) {
    if (keyType == null) {
      error("keyType is null")
      return
    }
    val keyTypeName = keyType.javaToKotlinType()
    val uniqueKeyConfigSetKmClass = UniqueKeyResourceSet.asTypeElement().toImmutableKmClass()
    uniqueKeyConfigSetKmClass.functions
      .forEach { func ->
        val funBuilder = FunSpec.builder(func.name)
        func.valueParameters.forEach { p ->
          val parameterType = p.typeName() ?: keyTypeName
          funBuilder.addParameter(p.name, parameterType)
        }
        val params = func.valueParameters.asSequence().map { it.name }.joinToString(", ")
        funBuilder.addModifiers(getModifiers(func))
          .addStatement(
            "return (underlying.getDelegatee() as %T<%T, %T>).%L(%L)",
            UniqueKeyResourceSet,
            keyTypeName,
            elem,
            func.name,
            params
          )
          .addAnnotation(JvmStatic::class)
        classBuilder.addFunction(funBuilder.build())
      }
  }

  private fun findGroupUniqueKeyType(elem: TypeElement): DeclaredType? {
    return elem.interfaces.asSequence()
      .map { it as DeclaredType }
      .find {
        it.asElement().toString() == GroupedUniqueKey.canonicalName
      }?.let { it.typeArguments[1] } as? DeclaredType
  }

  private fun findGroupIdType(elem: TypeElement): DeclaredType? {
    return elem.interfaces.asSequence()
      .map { it as DeclaredType }
      .find {
        val typeName = it.asElement().toString()
        typeName == Grouped.canonicalName || typeName == GroupedUniqueKey.canonicalName
      }?.let { it.typeArguments[0] } as? DeclaredType
  }

  private fun implementGrouped(
    elem: TypeElement,
    groupIdType: DeclaredType?,
    groupUniqueKeyType: DeclaredType?,
    classBuilder: TypeSpec.Builder
  ) {
    if (groupIdType == null) {
      error("groupIdType is null.")
      return
    }
    val isGroupedUniqueKey = GroupedUniqueKey.asTypeElement().asType().isAssignableFrom(elem.asType())
    val groupIdTypeName = groupIdType.javaToKotlinType()

    val genericGroupedResourceSet = GroupedResourceSet.parameterizedBy(groupIdTypeName, elem.asType().asTypeName())
    if (isGroupedUniqueKey) {
      val returnType =
        GroupUniqueKeyResourceSet.parameterizedBy(elem.asType().asTypeName(), groupUniqueKeyType!!.javaToKotlinType())
      val getGroupOrNull = FunSpec.builder("getGroupOrNull")
        .addAnnotation(JvmStatic::class)
        .addParameter("groupId", groupIdTypeName)
        .returns(returnType.copy(true))
        .addStatement(
          "return (underlying.getDelegatee() as %T).getGroupOrNull(groupId) as? %T",
          genericGroupedResourceSet,
          returnType
        ).build()
      val getGroup = FunSpec.builder("getGroup")
        .addAnnotation(JvmStatic::class)
        .addParameter("groupId", groupIdTypeName)
        .addStatement("return %T.ofNullable(getGroupOrNull(groupId))", Optional::class)
        .build()
      val getGroupOrThrow = FunSpec.builder("getGroupOrThrow")
        .addAnnotation(JvmStatic::class)
        .addParameter("groupId", groupIdTypeName)
        .addStatement("""return getGroupOrNull(groupId) ?: throw NoSuchElementException("group: ${'$'}groupId")""")
        .build()
      val groupMap = FunSpec.builder("groupMap")
        .addAnnotation(JvmStatic::class)
        .addStatement(
          "return (underlying.getDelegatee() as %T).groupMap() as %T",
          genericGroupedResourceSet,
          Map::class.asClassName().parameterizedBy(groupIdTypeName, returnType)
        )
        .build()
      val containsGroup = FunSpec.builder("containsGroup")
        .addAnnotation(JvmStatic::class)
        .addParameter("groupId", groupIdTypeName)
        .addStatement(
          "return (underlying.getDelegatee() as %T).containsGroup(groupId)",
          genericGroupedResourceSet
        )
        .build()
      classBuilder
        .addFunction(getGroup)
        .addFunction(getGroupOrThrow)
        .addFunction(getGroupOrNull)
        .addFunction(groupMap)
        .addFunction(containsGroup)
    } else {
      val immutableKmClass = GroupedResourceSet.asTypeElement().toImmutableKmClass()
      immutableKmClass.functions.asSequence()
        .forEach { func ->
          val funBuilder = FunSpec.builder(func.name)
          func.valueParameters.forEach { p ->
            val parameterType = p.typeName() ?: groupIdTypeName
            funBuilder.addParameter(p.name, parameterType)
          }
          val params = func.valueParameters.asSequence().map { it.name }.joinToString(", ")
          funBuilder.addStatement(
            "return (underlying.getDelegatee() as %T).%L(%L)",
            genericGroupedResourceSet,
            func.name,
            params
          )
          funBuilder.addModifiers(getModifiers(func))
            .addAnnotation(JvmStatic::class)
          classBuilder.addFunction(funBuilder.build())
        }
    }
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
          "return (underlying.getDelegatee() as %T<%T, %T>).extension()",
          ExtensionResourceSet,
          extensionType,
          elem
        )
        .addAnnotation(JvmStatic::class)
        .build()
    )
  }

  private fun implementSingleton(type: TypeElement, classBuilder: TypeSpec.Builder) {
    val resourceTypeName = type.asClassName()
    val genericSingletonResourceSet = SingletonResourceSet.parameterizedBy(resourceTypeName)
    val genericDelegatedResourceSet = DelegatedResourceSet.parameterizedBy(resourceTypeName)
    classBuilder.addProperty(
      PropertySpec
        .builder("underlying", genericDelegatedResourceSet, KModifier.PRIVATE)
        .initializer(
          "%T.getOrThrow(%T::class.java)",
          DelegatedResourceSet,
          type.asType()
        )
        .build()
    )

    classBuilder
      .addFunction(
        FunSpec.builder("get")
          .addModifiers(KModifier.PUBLIC)
          .returns(type.asClassName())
          .addStatement(
            "return (underlying.getDelegatee() as %T).get()",
            genericSingletonResourceSet
          )
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
            .addStatement("return get().%L", p.simpleName.toString())
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
          funcBuilder.addStatement("return get().%L(%L)", funName, params)
          classBuilder.addFunction(funcBuilder.build())
        }
      }
  }

  private fun isSingleton(elem: TypeElement): Boolean {
    if (AbstractConfig.asTypeElement().isAssignableFrom(elem)) {
      return true
    }
    return elem.isAnnotationPresent(SingletonResource)
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
