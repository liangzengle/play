package play.codegen

import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter

internal fun DeclaredType.asTypeElement() = asElement() as TypeElement

internal fun Element.isPublic() = this.modifiers.contains(Modifier.PUBLIC)

internal fun Element.isStatic() = this.modifiers.contains(Modifier.STATIC)


internal fun TypeElement.listMethods(): MutableList<ExecutableElement> {
  return ElementFilter.methodsIn(enclosedElements)
}

internal fun TypeElement.listVariables(): MutableList<VariableElement> {
  return ElementFilter.fieldsIn(enclosedElements)
}
