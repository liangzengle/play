package play.example.game.app.module.item.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import play.example.game.app.module.common.config.CommonSetting
import play.example.game.app.module.item.domain.ItemType
import play.example.game.app.module.reward.config.RawReward
import play.res.*
import javax.validation.Valid

/**
 * 物品配置
 * @author LiangZengle
 */
@ResourcePath("Item")
class ItemResource : AbstractResource(), ItemLikeResource, ExtensionKey<ItemResourceExtension>, Grouped<ItemType> {
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

  override fun groupId(): ItemType = type

  override fun postInitialize(resourceSetSupplier: ResourceSetSupplier, errors: MutableCollection<String>) {
    println("ItemConfig postInitialize: $this")
    val commonSetting = resourceSetSupplier.getSingleton(CommonSetting::class.java)
    bagFullMailId = commonSetting.get().bagFullMailId
  }

  override fun toString(): String {
    return "ItemConfig($id, $name, $desc, $numberValue, $jsonValue, $rewards)"
  }
}

class ItemResourceExtension(elems: List<ItemResource>) : ResourceExtension<ItemResource>(elems)
