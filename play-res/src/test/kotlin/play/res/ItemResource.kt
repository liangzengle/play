package play.res

import play.util.collection.IntTuple
import play.util.collection.IntTuple2

class ItemResource(id: Int, val key1: Int, val key2: Int, val groupId: Int, val keyInGroup: Int) : AbstractResource(id),
  UniqueKey<IntTuple2>,
  GroupedUniqueKey<Int, Int>,
  ExtensionKey<ItemResourceExtension> {

  override fun key(): IntTuple2 = IntTuple(key1, key2)

  override fun groupBy(): Int = groupId

  override fun keyInGroup(): Int = keyInGroup
}

class ItemResourceExtension(list: List<ItemResource>) : ResourceExtension<ItemResource>(list)
