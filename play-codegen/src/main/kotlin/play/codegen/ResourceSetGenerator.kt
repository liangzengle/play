package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.isData
import com.squareup.kotlinpoet.metadata.toKmClass
import java.io.File
import java.util.*
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter

@AutoService(Processor::class)
class ResourceSetGenerator : PlayAnnotationProcessor() {

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf("play.res.ResourcePath", "play.res.SingletonResource")
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    roundEnv
      .subtypesOf(AbstractResource)
      .filterNot {
        it.isAnnotationPresent(Ignore) || it.isAnnotationPresent(DisableCodegen)
      }.forEach { elem ->
        val simpleName = elem.simpleName.toString()
        val isSingleton = isSingleton(elem)
        val postfix = if (isSingleton) "Conf" else "Set"
        val objectName = simpleName + postfix
        val objectBuilder = TypeSpec
          .objectBuilder(objectName)
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
        if (isSingleton) {
          implementSingleton(elem, objectBuilder)
        } else {
          implementResourceSet(elem, objectBuilder)
          implementUniqueKey(elem, objectBuilder)
          implementGrouped(elem, objectBuilder)
          implementExtension(elem, objectBuilder)
        }
        val file = File(generatedSourcesRoot)
        file.mkdir()
        val pkg = processingEnv.elementUtils.getPackageOf(elem).toString()
        val fileBuilder = FileSpec.builder(pkg, objectName)
        fileBuilder
          .addType(objectBuilder.build())
          .build()
          .writeTo(file)
      }
    return true
  }

  private fun implementResourceSet(elem: TypeElement, classBuilder: TypeSpec.Builder) {
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

    val typeTable = mapOf("T" to elem.asClassName())
    for (element in ResourceSet.asTypeElement().enclosedElements) {
      if (element !is ExecutableElement) {
        continue
      }
      val functionName = element.simpleName.toString()
      val funcBuilder = FunSpec.builder(functionName).addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class)
      for (parameter in element.parameters) {
        val paramType = resolveTypeVariables(parameter.asType().javaToKotlinType(), typeTable)
        funcBuilder.addParameter(parameter.simpleName.toString(), paramType)
      }
      funcBuilder.addStatement("return underlying.%L(%L)", functionName, toParamStr(element.parameters))
      classBuilder.addFunction(funcBuilder.build())
    }
  }

  private fun implementUniqueKey(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    fun findUniqueKeyType(elem: TypeElement): DeclaredType? {
      return elem.interfaces.asSequence()
        .map { it as DeclaredType }
        .find {
          val interfaceName = it.asElement().toString()
          interfaceName == UniqueKey.canonicalName || interfaceName == ComparableUniqueKey.canonicalName
        }?.let { it.typeArguments[0] } as? DeclaredType
    }

    val uniqueKeyType = findUniqueKeyType(elem) ?: return
    val keyTypeName = uniqueKeyType.javaToKotlinType()
    val uniqueKeyTypeElement = UniqueKeyResourceSet.asTypeElement()
    val resourceTypeName = elem.asType().asTypeName()
    val typeTable = mapOf("K" to keyTypeName, "T" to resourceTypeName)
    for (element in uniqueKeyTypeElement.enclosedElements) {
      if (element !is ExecutableElement) {
        continue
      }
      val functionName = element.simpleName.toString()
      val funcBuilder = FunSpec.builder(functionName).addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class)
      for (parameter in element.parameters) {
        val paramType = resolveTypeVariables(parameter.asType().asTypeName(), typeTable)
        funcBuilder.addParameter(ParameterSpec(parameter.simpleName.toString(), paramType))
      }
      funcBuilder.addStatement(
        "return (underlying.getDelegatee() as %T<%T, %T>).%L(%L)",
        UniqueKeyResourceSet,
        keyTypeName,
        elem,
        functionName,
        toParamStr(element.parameters)
      )
      classBuilder.addFunction(funcBuilder.build())
    }
  }

  private fun implementGrouped(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    fun findGroupedUniqueKeyType(elem: TypeElement): DeclaredType? {
      return elem.interfaces.asSequence()
        .map { it as DeclaredType }
        .find {
          it.asElement().toString() == GroupedUniqueKey.canonicalName
        }?.let { it.typeArguments[1] } as? DeclaredType
    }

    fun findGroupIdType(elem: TypeElement): DeclaredType? {
      return elem.interfaces.asSequence()
        .map { it as DeclaredType }
        .find {
          val typeName = it.asElement().toString()
          typeName == Grouped.canonicalName || typeName == GroupedUniqueKey.canonicalName
        }?.let { it.typeArguments[0] } as? DeclaredType
    }

    val groupIdType = findGroupIdType(elem)?.javaToKotlinType() ?: return
    val resourceTypeName = elem.asType().asTypeName()

    val typeTable = hashMapOf<String, TypeName>()
    typeTable["G"] = groupIdType
    typeTable["T"] = resourceTypeName

    val groupedUniqueKeyType = findGroupedUniqueKeyType(elem)?.javaToKotlinType()
    var parameterizedUniqueKeyResourceGroup: TypeName? = null
    if (groupedUniqueKeyType != null) {
//      typeTable["K"] = groupUniqueKeyType
      parameterizedUniqueKeyResourceGroup =
        UniqueKeyResourceGroup.parameterizedBy(resourceTypeName, groupedUniqueKeyType)
    }

    val parameterizeResourceGroup = ResourceGroup.parameterizedBy(resourceTypeName)

    val groupedResourceSetTypeElement = GroupedResourceSet.asTypeElement()
    for (element in groupedResourceSetTypeElement.enclosedElements) {
      if (element !is ExecutableElement) {
        continue
      }
      val functionName = element.simpleName.toString()
      val originalReturnType = resolveTypeVariables(element.returnType.javaToKotlinType(), typeTable)
      val returnType = if (parameterizedUniqueKeyResourceGroup != null) {
        replaceType(originalReturnType, parameterizeResourceGroup, parameterizedUniqueKeyResourceGroup)
      } else originalReturnType
      val funcBuilder =
        FunSpec.builder(functionName).addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class).returns(returnType)
      for (parameter in element.parameters) {
        val paramType = resolveTypeVariables(parameter.asType().asTypeName(), typeTable)
        funcBuilder.addParameter(parameter.simpleName.toString(), paramType)
      }
      if (originalReturnType != returnType) {
        funcBuilder
          .addStatement(
            "return (underlying.getDelegatee() as %T).%L(%L) as %T",
            GroupedResourceSet.parameterizedBy(groupIdType, resourceTypeName),
            functionName,
            toParamStr(element.parameters),
            returnType
          )
      } else {
        funcBuilder
          .addStatement(
            "return (underlying.getDelegatee() as %T).%L(%L)",
            GroupedResourceSet.parameterizedBy(groupIdType, resourceTypeName),
            functionName,
            toParamStr(element.parameters)
          )
      }
      classBuilder.addFunction(funcBuilder.build())
    }
  }

  private fun implementExtension(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    if (!ExtensionKey.asTypeElement().asType().isAssignableFrom(elem.asType())) {
      return
    }
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
        .addAnnotation(JvmStatic::class)
        .returns(extensionType.asTypeName())
        .addStatement(
          "return (underlying.getDelegatee() as %T<%T, %T>).extension()",
          ExtensionResourceSet,
          extensionType,
          elem
        )
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
        .initializer("%T.getOrThrow(%T::class.java)", DelegatedResourceSet, type.asType())
        .build()
    )

    classBuilder
      .addFunction(
        FunSpec.builder("get").addModifiers(KModifier.PUBLIC).returns(type.asClassName())
          .addStatement("return (underlying.getDelegatee() as %T).get()", genericSingletonResourceSet)
          .build()
      )
    val getters = ElementFilter.fieldsIn(type.enclosedElements).asSequence().map {
      val name = it.simpleName.toString()
      val prefix = if (it.javaToKotlinType().toString() == "Boolean") "is" else "get"
      (prefix + name.capitalize()) to it
    }.toMap()
    val kmClass = type.toKmClass()
    val isDataClass = kmClass.isData
    for (elem in ElementFilter.methodsIn(type.enclosedElements)) {
      if (elem !is ExecutableElement) {
        continue
      }
      if (!elem.isPublic() || elem.isStatic()) {
        continue
      }
      if (isInitializeFunction(elem) || (isDataClass && isDataClassFunction(elem))) {
        continue
      }
      if (isObjectMethod(elem)) {
        continue
      }
      val functionName = elem.simpleName.toString()
      if (getters.contains(functionName)) {
        val p = getters[functionName]!!
        val getter = FunSpec.builder("get()")
          .addStatement("return get().%L", p.simpleName)
          .build()
        val property =
          PropertySpec.builder(p.simpleName.toString(), p.asType().javaToKotlinType()).getter(getter)
            .addAnnotation(JvmStatic::class)
            .build()
        classBuilder.addProperty(property)
      } else {
        val funcBuilder = FunSpec.builder(functionName).addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class)
        for (parameter in elem.parameters) {
          funcBuilder.addParameter(parameter.simpleName.toString(), parameter.javaToKotlinType())
        }
        funcBuilder.addStatement("return get().%L(%L)", functionName, toParamStr(elem.parameters))
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

  private fun isInitializeFunction(elem: ExecutableElement): Boolean {
    val name = elem.simpleName.toString()
    if (name != "initialize") {
      return false
    }
    val parameters = elem.parameters
    return parameters.size == 2
  }

  private fun isDataClassFunction(elem: ExecutableElement): Boolean {
    return (elem.modifiers.contains(Modifier.FINAL)
      && elem.simpleName.startsWith("component")
      && elem.simpleName.substring("component".length).toIntOrNull() != null
      )
      || (elem.simpleName.contentEquals("copy") && elem.returnType == elem.enclosingElement.asType())
  }
}
