package play.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.ImmutableKmValueParameter
import javax.lang.model.element.Name
import kotlinx.metadata.KmClassifier

fun ImmutableKmType?.toTypeName(): TypeName {
  if (this == null) {
    return STAR
  }
  val typeName = when (val classifier = this.classifier) {
    is KmClassifier.Class -> ClassName.bestGuess(classifier.name.replace('/', '.'))
    is KmClassifier.TypeAlias -> ClassName.bestGuess(classifier.name.replace('/', '.'))
    is KmClassifier.TypeParameter -> arguments[classifier.id].type.toTypeName() as ClassName
  }
  if (arguments.isEmpty()) {
    return typeName
  }
  return typeName.parameterizedBy(arguments.map { it.type.toTypeName() })
}

fun ImmutableKmValueParameter.typeName(): TypeName? {
  val type = this.type ?: return STAR
  val typeName = when (val classifier = type.classifier) {
    is KmClassifier.Class -> ClassName.bestGuess(classifier.name.replace('/', '.'))
    is KmClassifier.TypeAlias -> ClassName.bestGuess(classifier.name.replace('/', '.'))
    is KmClassifier.TypeParameter -> null
  } ?: return null
  return if (type.arguments.isEmpty()) {
    typeName
  } else {
    typeName.parameterizedBy(type.arguments.map { it.type.toTypeName() })
  }
}
