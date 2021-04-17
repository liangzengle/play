package play.codegen

import com.google.auto.service.AutoService
import java.util.*
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

/**
 *
 * @author LiangZengle
 */
@AutoService(Processor::class)
class EnumTypeAnnotationProcessor : PlayAnnotationProcessor() {
  override fun getSupportedAnnotationTypes(): Set<String> {
    return Collections.singleton(EnumType::class.qualifiedName!!)
  }

  private val enumTypeMap = mutableMapOf<String, MutableList<String>>()

  private val enumInstanceMap = mutableMapOf<String, MutableSet<String>>()

  override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
    return try {
      processImpl(roundEnv)
    } catch (e: Exception) {
      error(e)
      true
    }
  }

  private fun processImpl(roundEnv: RoundEnvironment): Boolean {
    if (roundEnv.processingOver()) {
      writeConfigFile()
      return true
    }
    val elements = roundEnv.getElementsAnnotatedWith(EnumType::class.asTypeElement())
    for (element in elements) {
      if (element.kind != ElementKind.ENUM) {
        error("$element which annotated with @EnumType is not a enum class.")
      }
      val typeElement = element as TypeElement
      val enumTypeName = typeElement.getAnnotation(EnumType::class.java).value.qualifiedName!!
      try {
        addEnum(enumTypeName, typeElement.qualifiedName.toString())
      } catch (e: Exception) {
        error(e)
      }
      typeElement.enclosedElements
        .asSequence()
        .filter { it.kind == ElementKind.ENUM_CONSTANT }
        .forEach { addEnumInstance(enumTypeName, it.simpleName.toString()) }
    }
    return true
  }

  private fun addEnumInstance(enumTypeName: String, enumConstant: String) {
    val enumConstants = enumInstanceMap[enumTypeName] ?: enumInstanceMap.computeIfAbsent(enumTypeName) { hashSetOf() }
    enumConstants.add(enumConstant)
  }

  private fun addEnum(enumTypeName: String, enumName: String) {
    val enums = enumTypeMap[enumTypeName] ?: enumTypeMap.computeIfAbsent(enumTypeName) { LinkedList() }
    enums.add(enumName)
  }

  private fun writeConfigFile() {
    for ((enumType, enums) in enumTypeMap) {
      appendServices(enumType, enums)
    }
  }
}
