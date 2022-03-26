package play.example.game.app.module.item.res

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import jakarta.validation.Valid
import play.example.game.app.module.common.res.CommonSetting
import play.example.game.app.module.item.domain.ItemType
import play.example.game.app.module.reward.res.RawReward
import play.res.*

/**
 * 物品配置
 * @author LiangZengle
 */
@ResourcePath("Item")
class ItemResource : AbstractResource(), ItemLikeResource, ExtensionKey<ItemResourceExtension>, UniqueKey<String>,
  GroupedWithUniqueKey<ItemType, Int> {
  override val name: String = ""

  private val desc = ""

  val type = ItemType.Normal

  val subtype = 0

  val numberValue = 0.0

  private val jsonValue: JsonNode = NullNode.instance

  @Valid
  val rewards = emptyList<RawReward>()

  var bagFullMailId = 0
    private set

  override fun key(): String {
    return id.toString()
  }

  override fun groupBy(): ItemType = type

  override fun keyInGroup(): Int = id

  override fun initialize(resourceSetProvider: ResourceSetProvider, errors: MutableCollection<String>) {
    println("ItemResource postInitialize: $this")
    val commonSetting = resourceSetProvider.getSingleton(CommonSetting::class.java)
    bagFullMailId = commonSetting.get().bagFullMailId
  }

  override fun toString(): String {
    return "ItemResource($id, $name, $desc, $numberValue, $jsonValue, $rewards)"
  }
}

class ItemResourceExtension(list: List<ItemResource>) : ResourceExtension<ItemResource>(list)
