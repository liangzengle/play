package play.codegen

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

@AutoService(Processor::class)
class EnumInterfaceProcessor : PlayAnnotationProcessor() {

  private val map = hashMapOf<TypeElement, MutableList<TypeElement>>()

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf(EnumInterface::class.java.name)
  }

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    if (roundEnv.processingOver()) {
      onProcessOver()
    } else {
      process(roundEnv)
    }
    return true
  }

  private fun process(roundEnv: RoundEnvironment) {
    val elements = roundEnv.getElementsAnnotatedWith(EnumInterface::class.java)
    for (element in elements) {
      val interfaceType = getAnnotationValue<DeclaredType>(element, EnumInterface::class.asClassName(), "value")!!.asTypeElement()
      map.computeIfAbsent(interfaceType) { arrayListOf() }.add(element.unsafeCast())
    }
  }

  private fun onProcessOver() {
    for ((k, v) in map) {
      generate(k, v)
    }
  }

  private fun generate(interfaceType: TypeElement, enumTypes: List<TypeElement>) {
    val enumConstantMap = hashMapOf<String, Element>()
    for (enumType in enumTypes) {
      if (enumType.kind != ElementKind.ENUM) {
        error("$enumType is not enum")
        continue
      }
      for (enclosedElement in enumType.enclosedElements) {
        if (enclosedElement.kind != ElementKind.ENUM_CONSTANT) {
          continue
        }
        val name = enclosedElement.simpleName.toString()
        val prev = enumConstantMap.put(name, enclosedElement)
        if (prev != null) {
          error("duplicated enum name: $name in ${enclosedElement.enclosingElement} and ${prev.enclosingElement}")
          break
        }
      }
    }
    val interfaceClassName = interfaceType.asClassName()
    val valueOfBuilder = FunSpec.builder("valueOf")
      .returns(interfaceClassName)
      .addAnnotation(JvmStatic::class)
      .addParameter("name", String::class)
    val valueOfBody = CodeBlock.builder()
    valueOfBody.beginControlFlow("return when(name)")
    for ((name, element) in enumConstantMap) {
      valueOfBody.addStatement("%S -> %T.%L", name, element.enclosingElement, name)
    }
    valueOfBody.addStatement("else -> throw %T()", IllegalArgumentException::class)
    valueOfBody.endControlFlow()
    val valueOf = valueOfBuilder.addCode(valueOfBody.build()).build()

    val valuesBuilder = FunSpec.builder("values")
      .returns(List::class.asClassName().parameterizedBy(interfaceClassName))
      .addAnnotation(JvmStatic::class)
    val valuesBody = CodeBlock.builder()
    valuesBody.add("return listOf(")
    var first = true
    for ((name, element) in enumConstantMap) {
      if (!first) {
        valuesBody.add(", ")
      }
      valuesBody.add("%T.%L", element.enclosingElement, name)
      first = false
    }
    valuesBody.add(")")
    val values = valuesBuilder.addCode(valuesBody.build()).build()

    val type = TypeSpec.objectBuilder(interfaceType.simpleName.toString() + "s")
      .addFunction(valueOf)
      .addFunction(values)
      .build()

    val file = File(generatedSourcesRoot)
    FileSpec.builder(interfaceType.getPackage(), type.name!!)
      .addType(type)
      .build()
      .writeTo(file)
  }
}
