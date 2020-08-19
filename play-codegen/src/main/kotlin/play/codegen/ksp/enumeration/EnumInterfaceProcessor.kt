package play.codegen.ksp.enumeration

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import play.codegen.*
import play.codegen.ksp.*
import java.util.*

class EnumInterfaceProcessor(environment: SymbolProcessorEnvironment) : AbstractSymbolProcessor(environment) {

  private val enumInterfaces: MutableMap<ClassName, MutableMap<String, ClassName>> = linkedMapOf()

  override fun process(): List<KSAnnotated> {
    val symbols = resolver.getSymbolsWithAnnotation(qualifiedName<EnumInterface>())
    symbols.filterIsInstance<KSClassDeclaration>().forEach { it.accept(Visitor(), Unit) }
    return emptyList()
  }

  override fun finish() {
    for ((interfaceType, enumMap) in enumInterfaces) {
      generate(interfaceType, enumMap)
    }
  }

  private fun generate(interfaceType: ClassName, enumConstantMap: Map<String, ClassName>) {
    val typeBuilder =
      TypeSpec.objectBuilder("${interfaceType.simpleName}s").addProperty(valuesProperty(interfaceType, enumConstantMap))
        .addFunction(valuesFunction()).addFunction(getByNameOrNullFunction(interfaceType, enumConstantMap))
        .addFunction(getByNameOrThrowFunction(interfaceType)).addFunction(containsNameFunction())

    val idEnum = resolver.getClassDeclarationOrNull(IdEnum.canonicalName)
    val enumInterface = resolver.getClassDeclarationOrNull(interfaceType.canonicalName)
    if (idEnum != null && enumInterface != null && resolver.isAssignable(idEnum, enumInterface)) {
      typeBuilder.addProperty(idToIndexMapProperty()).addFunction(getByIdOrNullFunction(interfaceType))
        .addFunction(getByIdOrThrowFunction(interfaceType)).addFunction(containsIdFunction())
    }

    write(typeBuilder.build(), interfaceType.packageName)
  }

  private fun idToIndexMapProperty(): PropertySpec {
    return PropertySpec.builder("idToIndexMap", IntIntMap)
      .initializer("%T.immutable.from(VALUES.indices, { VALUES[it].id() }, { it })", IntIntMaps).build()
  }

  private fun getByIdOrNullFunction(interfaceType: ClassName): FunSpec {
    return FunSpec.builder("getByIdOrNull").addParameter("id", Int::class).returns(interfaceType)
      .addStatement("val idx = idToIndexMap.getIfAbsent(id, -1)").addStatement("if (idx == -1) return null")
      .addStatement("return VALUES[idx]").build()
  }

  private fun getByIdOrThrowFunction(interfaceType: ClassName): FunSpec {
    return FunSpec.builder("getByIdOrThrow").addParameter("id", Int::class).returns(interfaceType)
      .addStatement("return getByIdOrNull(id) ?: %T(id.toString())", IllegalArgumentException::class).build()
  }

  private fun containsIdFunction(): FunSpec {
    return FunSpec.builder("contains").returns(Boolean::class).addAnnotation(JvmStatic::class)
      .addParameter("id", Int::class).addStatement("return getByIdOrNull(name) != null").build()
  }

  private fun containsNameFunction(): FunSpec {
    return FunSpec.builder("contains").returns(Boolean::class).addAnnotation(JvmStatic::class)
      .addParameter("name", String::class).addStatement("return getByNameOrNull(name) != null").build()
  }

  private fun getByNameOrThrowFunction(interfaceType: ClassName): FunSpec {
    return FunSpec.builder("getByNameOrThrow").returns(interfaceType)
      .addAnnotation(JvmStatic::class).addParameter("name", String::class)
      .addStatement("return getByNameOrNull(name) ?: throw %T(name)", IllegalArgumentException::class).build()
  }

  private fun getByNameOrNullFunction(interfaceType: ClassName, enumConstantMap: Map<String, ClassName>): FunSpec {
    val builder =
      FunSpec.builder("getByNameOrNull").returns(interfaceType.copy(nullable = true)).addAnnotation(JvmStatic::class)
        .addParameter("name", String::class)
    val body = CodeBlock.builder()
    body.beginControlFlow("return when(name)")
    for ((enumName, enumClass) in enumConstantMap) {
      body.addStatement("%S -> %T.%L", enumName, enumClass, enumName)
    }
    body.addStatement("else -> null")
    body.endControlFlow()
    return builder.addCode(body.build()).build()
  }

  private fun valuesFunction(): FunSpec {
    return FunSpec.builder("values").addAnnotation(JvmStatic::class).addStatement("return VALUES").build()
  }

  private fun valuesProperty(interfaceType: ClassName, enumConstantMap: Map<String, ClassName>): PropertySpec {
    val builder = PropertySpec.builder("VALUES", List::class.asClassName().parameterizedBy(interfaceType))
      .addAnnotation(JvmStatic::class)
    val initCode = CodeBlock.builder()
    initCode.add("listOf(")
    var first = true
    for ((enumName, enumClass) in enumConstantMap) {
      if (!first) {
        initCode.add(", ")
      }
      initCode.add("%T.%L", enumClass, enumName)
      first = false
    }
    initCode.add(")")
    builder.initializer(initCode.build())
    return builder.build()
  }

  private inner class Visitor : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      if (classDeclaration.classKind == ClassKind.ENUM_CLASS) {
        classDeclaration.enumConstants().forEach { visitClassDeclaration(it, data) }
      } else if (classDeclaration.classKind == ClassKind.ENUM_ENTRY) {
        val enumName = classDeclaration.simpleName.getShortName()
        val enumClass = classDeclaration.parentDeclaration as KSClassDeclaration
        val interfaceType =
          enumClass.getAnnotation(EnumInterface::class).getValue<KSType>("value").declaration as KSClassDeclaration
        val enumMap = enumInterfaces.computeIfAbsent(interfaceType.toClassName()) { TreeMap() }
        val prev = enumMap.putIfAbsent(enumName, enumClass.toClassName())
        if (prev != null) {
          logger.error("Duplicated Enum Name: ${interfaceType.simpleName.getShortName()}.$enumName")
        }
      }
    }

    override fun visitFile(file: KSFile, data: Unit) {
      file.declarations.filterIsInstance<KSClassDeclaration>().forEach { visitDeclaration(it, data) }
    }
  }
}
