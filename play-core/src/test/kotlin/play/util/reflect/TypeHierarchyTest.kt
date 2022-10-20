package play.util.reflect

import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import java.io.Serializable

/**
 *
 *
 * @author LiangZengle
 */
internal class TypeHierarchyTest {

    @Test
    fun get() {
      assertSetEquals(
        TypeHierarchy.get(ArrayList::class.java, includeSelf = true, includeObject = true),
        setOf(
          ArrayList::class.java,
          java.util.AbstractList::class.java,
          java.util.AbstractCollection::class.java,
          java.util.List::class.java,
          java.util.Collection::class.java,
          java.lang.Iterable::class.java,
          RandomAccess::class.java,
          Cloneable::class.java,
          Serializable::class.java,
          Any::class.java
        )
      )

      assertSetEquals(
        TypeHierarchy.get(ArrayList::class.java, includeSelf = false, includeObject = true),
        setOf(
//          ArrayList::class.java,
          java.util.AbstractList::class.java,
          java.util.AbstractCollection::class.java,
          java.util.List::class.java,
          java.util.Collection::class.java,
          java.lang.Iterable::class.java,
          RandomAccess::class.java,
          Cloneable::class.java,
          Serializable::class.java,
          Any::class.java
        )
      )

      assertSetEquals(
        TypeHierarchy.get(ArrayList::class.java, includeSelf = true, includeObject = false),
        setOf(
          ArrayList::class.java,
          java.util.AbstractList::class.java,
          java.util.AbstractCollection::class.java,
          java.util.List::class.java,
          java.util.Collection::class.java,
          java.lang.Iterable::class.java,
          RandomAccess::class.java,
          Cloneable::class.java,
          Serializable::class.java,
//          Any::class.java
        )
      )

      assertSetEquals(
        TypeHierarchy.get(ArrayList::class.java, includeSelf = false, includeObject = false),
        setOf(
//          ArrayList::class.java,
          java.util.AbstractList::class.java,
          java.util.AbstractCollection::class.java,
          java.util.List::class.java,
          java.util.Collection::class.java,
          java.lang.Iterable::class.java,
          RandomAccess::class.java,
          Cloneable::class.java,
          Serializable::class.java,
//          Any::class.java
        )
      )
    }

  private fun assertSetEquals(expected: Set<Any>, given: Set<Any>) {
    if (expected.size != given.size) {
      throw AssertionFailedError()
    }
    for (any in expected) {
      if (!given.contains(any)) {
        throw AssertionFailedError()
      }
    }
  }
}
