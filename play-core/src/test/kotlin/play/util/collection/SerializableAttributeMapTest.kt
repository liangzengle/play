package play.util.collection

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import play.util.json.Json

/**
 * @author LiangZengle
 */
internal class SerializableAttributeMapTest {
  private val key1 = SerializableAttributeKey.valueOf<Int>("key1")

  @Test
  fun attr() {
    val map = SerializableAttributeMap()
    map.attr(key1).setValue(1)

    val key2 = SerializableAttributeKey.valueOf<Int>("key1")
    Assertions.assertSame(key2, key1)
    Assertions.assertEquals(1, map.attr(key1).getValue())
    Assertions.assertEquals(map.attr(key1).getValue(), map.attr(key2).getValue())

    val str = Json.toJsonString(map)
    val map2 = Json.readValueAs<SerializableAttributeMap>(str)

    Assertions.assertEquals(map2.attr(key1).getValue(), map.attr(key1).getValue())
  }
}
