package play.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import kotlinx.metadata.KmClassifier

fun ImmutableKmType?.toTypeName(): TypeName {
  if (this == null) {
    return STAR
  }
  val typeName = when (val classifier = this.classifier) {
    is KmClassifier.Class -> ClassName.bestGuess(classifier.name.replace('/', '.'))
    is KmClassifier.TypeAlias -> ClassName.bestGuess(classifier.name.replace('/', '.'))
    is KmClassifier.TypeParameter -> throw IllegalArgumentException("don't know how get TypeName from KmClassifier.TypeParameter")
  }
  if (arguments.isEmpty()) {
    return typeName
  }
  return typeName.parameterizedBy(arguments.map { it.type.toTypeName() })
}

