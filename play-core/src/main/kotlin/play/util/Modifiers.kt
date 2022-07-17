package play.util

import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Modifier

fun Class<*>.isPublic() = Modifier.isPublic(modifiers)

fun Class<*>.isProtected() = Modifier.isProtected(modifiers)

fun Class<*>.isPrivate() = Modifier.isPrivate(modifiers)

fun Class<*>.isAbstract() = Modifier.isAbstract(modifiers)

fun Class<*>.isStatic() = Modifier.isStatic(modifiers)

fun Class<*>.isFinal() = Modifier.isFinal(modifiers)

fun Class<*>.isStrict() = Modifier.isStrict(modifiers)

fun Member.isPublic() = Modifier.isPublic(modifiers)

fun Member.isProtected() = Modifier.isProtected(modifiers)

fun Member.isPrivate() = Modifier.isPrivate(modifiers)

fun Member.isAbstract() = Modifier.isAbstract(modifiers)

fun Member.isStatic() = Modifier.isStatic(modifiers)

fun Member.isFinal() = Modifier.isFinal(modifiers)

fun Member.isStrict() = Modifier.isStrict(modifiers)

fun Field.isTransient() = Modifier.isTransient(modifiers)
