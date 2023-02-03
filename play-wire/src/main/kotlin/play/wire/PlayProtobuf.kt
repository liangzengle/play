package play.wire

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.internal.parser.OptionElement

internal fun MessageType.getImplementations(): List<TypeName> {
  return options.elements.asSequence()
    .filter { it.kind == OptionElement.Kind.OPTION && it.name == "play.play_message_option" }
    .map { it.value as OptionElement }
    .filter { it.name == "implements" }
    .map { ClassName.bestGuess(it.value.toString()) }
    .toList()
}

internal fun Field.isOverride(): Boolean {
  return options.elements.asSequence()
    .filter { it.kind == OptionElement.Kind.OPTION && it.name == "play.play_field_option" }
    .map { it.value as OptionElement }
    .any { it.name == "override" && it.value == "true" }
}
