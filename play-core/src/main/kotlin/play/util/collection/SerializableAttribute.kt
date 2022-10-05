package play.util.collection

import java.util.function.Supplier

/**
 *
 * @author LiangZengle
 */
interface SerializableAttribute<T> {

  fun key(): SerializableAttributeKey<T>

  fun getValue(): T?

  fun computeIfAbsent(supplier: Supplier<T>): T

  fun setValue(value: T?)
}
