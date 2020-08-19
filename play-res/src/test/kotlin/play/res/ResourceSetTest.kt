package play.res

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ResourceSetTest {

  @Suppress("UNCHECKED_CAST")
  private val resourceClass = ItemResource::class.java as Class<AbstractResource>

  @Test
  fun testCreate() {
    assertThrows<InvalidResourceException> {
      val elems = listOf(
        ItemResource(1, 1, 1, 1, 1),
        ItemResource(1, 1, 1, 1, 1)
      )
      ResourceHelper.createResourceSet(resourceClass, elems)
    }

    assertThrows<InvalidResourceException> {
      val elems = listOf(
        ItemResource(1, 1, 1, 1, 1),
        ItemResource(2, 1, 1, 1, 1)
      )
      ResourceHelper.createResourceSet(resourceClass, elems)
    }

    assertThrows<InvalidResourceException> {
      val elems = listOf(
        ItemResource(1, 1, 1, 1, 1),
        ItemResource(2, 2, 1, 1, 1)
      )
      ResourceHelper.createResourceSet(resourceClass, elems)
    }

    assertDoesNotThrow {
      val elems = listOf(
        ItemResource(1, 1, 1, 1, 1),
        ItemResource(2, 2, 1, 1, 2)
      )
      ResourceHelper.createResourceSet(resourceClass, elems)
    }
  }
}
