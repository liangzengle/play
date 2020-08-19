package play.codegen

import com.google.auto.service.AutoService
import com.google.inject.Module
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.vavr.kotlin.option
import play.config.*
import play.inject.guice.GuiceModule
import java.io.File
import java.util.*
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod

@AutoService(Processor::class)
class ConfigSetGenerator : PlayAnnotationProcessor() {
  private val normal = 1
  private val hasUniqueKey = 2
  private val hasGrouped = 4
  private val hasExtension = 8

  override fun getSupportedAnnotationTypes0(): Set<KClass<out Annotation>> {
    return emptySet()
  }

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf("javax.inject.Singleton", "com.google.inject.Singleton")
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val configClassNames = TreeSet<ClassName>()
    roundEnv.subtypesOf(AbstractConfig::class).filterNot {
      it.isAnnotationPresent(Ignore::class)
    }.forEach { elem ->
      val interfaces = elem.interfaces.fold(normal) { flag, mirror ->
        when ((mirror as DeclaredType).asElement().toString()) {
          UniqueKey::class.java.name, ComparableUniqueKey::class.java.name -> flag + hasUniqueKey
          Grouped::class.java.name -> flag + hasGrouped
          ExtensionKey::class.java.name -> flag + hasExtension
          else -> flag
        }
      }
      val simpleName = elem.simpleName.toString()
      val objectBuilder = TypeSpec
        .objectBuilder(simpleName + "Set")
        .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
      objectBuilder.addProperty(
        PropertySpec
          .builder("delegatee", Any::class, KModifier.PRIVATE)
          .initializer("%T.get(%T::class.java)", DelegatedConfigSet::class, elem.asType())
          .build()
      )
      if (isSingleton(elem)) {
        implementSingleton(elem, objectBuilder)
      } else {
        implementBasics(elem, objectBuilder)
        if (hasUniqueKey(interfaces)) {
          implementUniqueKey(elem, objectBuilder)
        }
        if (hasGrouped(interfaces)) {
          implementGrouped(elem, objectBuilder)
          if (hasUniqueKey(interfaces)) {
            implementGetByKeyFromGroup(elem, objectBuilder)
          }
        }
        if (hasExtension(interfaces)) {
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
      configClassNames.add(elem.asClassName())
    }
    if (configClassNames.isNotEmpty()) {
//      val injectModule = buildInjectModule(configClassNames)
//      val file = File(generatedSourcesRoot)
//      FileSpec.builder("", injectModule.name!!).addType(injectModule).build().writeTo(file)
    }
    return false
  }

  private fun buildInjectModule(configClasses: Set<ClassName>): TypeSpec {
    val func = FunSpec.builder("configure").addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
    configClasses.forEach { configClass ->
      func.addStatement(
        "bind<%T>().toProvider(%T(%T::class.java))",
        ClassName(configClass.packageName, configClass.simpleName + "Set"),
        AnnotationSpec::class,
        configClass
      )
    }
    return TypeSpec.classBuilder("ConfigSetGuiceModule")
      .superclass(GuiceModule::class.asClassName())
      .addAnnotation(
        AnnotationSpec.builder(AutoService::class.asTypeName()).addMember(
          "%T::class",
          Module::class.asTypeName()
        ).build()
      )
      .addFunction(func.build())
      .build()
  }

  private fun implementBasics(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    BasicConfigSet::class.functions.asSequence()
      .filter {
        it.javaMethod?.declaringClass == BasicConfigSet::class.java
      }
      .forEach { func ->
        val funBuilder = FunSpec.builder(func.name)
        func.parameters.forEach { p ->
          if (p.name != null) {
            funBuilder.addParameter(p.name!!, p.type.asTypeName())
          }
        }
        val params = func.parameters.asSequence().filter { it.name != null }.map { it.name }.joinToString(", ")
        funBuilder.addModifiers(getModifiers(func))
//          .returns(returnType)
          .addStatement(
            "return (delegatee as %T<%T>).%L(%L)",
            BasicConfigSet::class,
            elem,
            func.name,
            params
          )
        classBuilder.addFunction(funBuilder.build())
      }
  }

  private fun implementUniqueKey(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    val keyTypeOpt = elem.interfaces.asSequence()
      .map { it as DeclaredType }
      .find {
        val interfaceName = it.asElement().toString()
        interfaceName == UniqueKey::class.qualifiedName || interfaceName == ComparableUniqueKey::class.qualifiedName
      }
      .option().map { it.typeArguments[0] }
    if (keyTypeOpt.isEmpty) {
      error("keyTypeOpt is empty")
      return
    }
    val keyType = keyTypeOpt.get() as DeclaredType
    UniqueKeyConfigSet::class.functions.asSequence()
      .filter { func ->
        func.javaMethod?.declaringClass == UniqueKeyConfigSet::class.java && !classBuilder.funSpecs.any { it.name == func.name }
      }
      .forEach { func ->
        val funBuilder = FunSpec.builder(func.name)
        func.parameters.forEach { p ->
          if (p.name != null) {
            val parameterType = when (val typeName = p.type.asTypeName()) {
              is ClassName -> typeName
              is TypeVariableName -> keyType.javaToKotlinType()
              is ParameterizedTypeName -> typeName.rawType.parameterizedBy(elem.asClassName())
              else -> null
            }
            if (parameterType == null) {
              error("unable to detect parameterType from ${p.type.asTypeName().javaClass}")
              return
            }
            funBuilder.addParameter(p.name!!, parameterType)
          }
        }
        val returnType = when (val typeName = func.returnType.asTypeName()) {
          is ClassName -> typeName
          is TypeVariableName -> elem.asClassName()
          is ParameterizedTypeName -> typeName.rawType.parameterizedBy(elem.asClassName())
          else -> null
        }
        if (returnType == null) {
          error("unable to detect returnType from ${func.returnType.asTypeName().javaClass}(${func.returnType.asTypeName()})")
          return
        }
        val params =
          func.parameters.asSequence().filter { it.name != null }.map { it.name }.joinToString(", ")
        funBuilder.addModifiers(getModifiers(func))
          .returns(returnType)
          .addStatement(
            "return (delegatee as %T<%T, %T>).%L(%L)",
            UniqueKeyConfigSet::class,
            keyType.javaToKotlinType(),
            elem,
            func.name,
            params
          )
        classBuilder.addFunction(funBuilder.build())
      }
  }

  private fun implementGrouped(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    val groupIdTypeOpt = elem.interfaces.asSequence()
      .map { it as DeclaredType }
      .find { it.asElement().toString() == Grouped::class.qualifiedName }
      .option().map { it.typeArguments[0] }
    if (groupIdTypeOpt.isEmpty) {
      error("extensionTypeOpt is empty")
      return
    }
    val keyTypeOpt = elem.interfaces.asSequence()
      .map { it as DeclaredType }
      .find {
        val interfaceName = it.asElement().toString()
        interfaceName == UniqueKey::class.qualifiedName || interfaceName == ComparableUniqueKey::class.qualifiedName
      }
      .option().map { it.typeArguments[0] }
    val groupIdType = groupIdTypeOpt.get() as DeclaredType
    GroupedConfigSet::class.functions.asSequence()
      .filter { func ->
        func.javaMethod?.declaringClass == GroupedConfigSet::class.java
      }
      .forEach { func ->
        val funBuilder = FunSpec.builder(func.name)
        func.parameters.forEach { p ->
          if (p.name != null) {
            val parameterType = when (val typeName = p.type.asTypeName()) {
              is ClassName -> typeName
              is TypeVariableName -> groupIdType.javaToKotlinType()
              is ParameterizedTypeName -> typeName.rawType.parameterizedBy(elem.asClassName())
              else -> null
            }
            if (parameterType == null) {
              error("unable to detect parameterType from ${p.type.asTypeName().javaClass}")
              return
            }
            funBuilder.addParameter(p.name!!, parameterType)
          }
        }
        val params =
          func.parameters.asSequence().filter { it.name != null }.map { it.name }.joinToString(", ")
        funBuilder.addModifiers(getModifiers(func))
          .addStatement(
            "return (delegatee as %T<%T, %T, %T>).%L(%L)",
            GroupedConfigSet::class,
            groupIdType.javaToKotlinType(),
            keyTypeOpt.map { it.javaToKotlinType() }.orNull ?: Int::class,
            elem,
            func.name,
            params
          )
        classBuilder.addFunction(funBuilder.build())
      }
  }

  private fun implementGetByKeyFromGroup(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    var groupType: TypeMirror? = null
    var keyType: TypeMirror? = null
    for (it in elem.interfaces) {
      it as DeclaredType
      val typeName = it.asElement().toString()
      if (typeName == Grouped::class.qualifiedName) {
        groupType = it.typeArguments[0]
      } else if (typeName == UniqueKey::class.qualifiedName || typeName == ComparableUniqueKey::class.qualifiedName) {
        keyType = it.typeArguments[0]
      }
      if (groupType != null && keyType != null) {
        break;
      }
    }
    if (groupType == null || keyType == null) {
      error("groupType=$groupType keyType=$keyType")
      return
    }
    val getByKeyFromGroup = FunSpec.builder("getByKeyFromGroup")
      .addParameter("groupId", groupType.asTypeName())
      .addParameter("key", keyType.asTypeName())
      .addStatement(
        "return (delegatee as %T<%T, %T, %T>).getGroup(groupId).flatMap{ it.getByKey(key) }",
        GroupedConfigSet::class,
        groupType.asTypeName(),
        keyType.asTypeName(),
        elem
      )
    classBuilder.addFunction(getByKeyFromGroup.build())
  }

  private fun implementExtension(elem: TypeElement, classBuilder: TypeSpec.Builder) {
    val extensionTypeOpt = elem.interfaces.asSequence()
      .map { it as DeclaredType }
      .find { it.asElement().toString() == ExtensionKey::class.qualifiedName }
      .option().map { it.typeArguments[0] }
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
          "return (delegatee as %T<%T, %T>).extension()",
          ExtensionConfigSet::class,
          extensionType,
          elem
        )
        .build()
    )
  }

  private fun implementSingleton(type: TypeElement, classBuilder: TypeSpec.Builder) {
    classBuilder
//            .addSuperinterface(SingletonResourceSet::class.asClassName().parameterizedBy(it.asClassName()))
      .addFunction(
        FunSpec.builder("get")
          .addModifiers(KModifier.PUBLIC)
          .returns(type.asClassName())
          .addStatement("return (delegatee as %T<%T>).get()", SingletonConfigSet::class, type)
          .build()
      )
    val getters = ElementFilter.fieldsIn(type.enclosedElements).asSequence().map {
      val name = it.simpleName.toString()
      val prefix = if (it.javaToKotlinType().toString() == "Boolean") "is" else "get"
      (prefix + name.capitalize()) to it
    }.toMap()
    ElementFilter.methodsIn(type.enclosedElements).asSequence()
      .filter {
        it.isPublic() && !it.isObjectMethod()
      }
      .forEach { elem ->
        val funName = elem.simpleName.toString()
        if (getters.contains(funName)) {
          val p = getters[funName]!!
          val getter = FunSpec.builder("get()")
            .addStatement(
              "return (delegatee as %T<%T>).get().%L",
              SingletonConfigSet::class,
              type,
              p.simpleName.toString()
            ).build()
          val property =
            PropertySpec.builder(p.simpleName.toString(), p.asType().javaToKotlinType()).getter(getter)
              .build()
          classBuilder.addProperty(property)
        } else {
          val funcBuilder = FunSpec.builder(funName).addModifiers(KModifier.PUBLIC)
          elem.parameters.forEach { p ->
            funcBuilder.addParameter(
              ParameterSpec.builder(
                p.simpleName.toString(),
                p.asType().javaToKotlinType()
              ).build()
            )
          }
          val params = elem.parameters.asSequence().map { p -> p.simpleName.toString() }.joinToString(", ")
          funcBuilder.addStatement(
            "return (delegatee as %T<%T>).get().%L(%L)",
            SingletonConfigSet::class,
            type,
            funName,
            params
          )
          classBuilder.addFunction(funcBuilder.build())
        }
      }
  }

  private fun hasUniqueKey(value: Int): Boolean = hasFlag(value, hasUniqueKey)
  private fun hasGrouped(value: Int): Boolean = hasFlag(value, hasGrouped)
  private fun hasExtension(value: Int): Boolean = hasFlag(value, hasExtension)
  private fun hasFlag(value: Int, flag: Int) = value and flag != 0

  private fun isSingleton(elem: TypeElement): Boolean {
    return elem.annotationMirrors.any {
      val typeName = it.annotationType.asTypeName()
      typeName == SingletonConfig::class.asTypeName() || typeName == Resource::class.asTypeName()
    }
  }

  private fun getModifiers(func: KFunction<*>): LinkedList<KModifier> {
    val mods = LinkedList<KModifier>()
/*        if (func.isExternal) {
            mods.add(KModifier.EXTERNAL)
        }*/
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
}
