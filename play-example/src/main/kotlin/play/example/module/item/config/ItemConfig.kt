package play.example.module.item.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import play.config.*
import play.example.module.item.domain.ItemType
import play.example.module.reward.config.RawReward
import javax.validation.Valid

/**
 * 物品配置
 * @author LiangZengle
 */
@ConfigPath("Item")
class ItemConfig : AbstractConfig(), ItemLikeConfig, ExtensionKey<ItemConfigExtension>, Grouped<ItemType> {
  override val name: String = ""

  private val desc = ""

  val type = ItemType.Normal

  val numberValue = 0.0

  private val jsonValue: JsonNode = NullNode.instance

  @Valid
  val rewards = emptyList<RawReward>()

  override fun groupId(): ItemType = type

  override fun postInitialize(configSetManager: ConfigSetManager, errors: MutableList<String>) {
  }

  override fun toString(): String {
    return "ItemConfig($id, $name, $desc, $numberValue, $jsonValue, $rewards)"
  }
}

class ItemConfigExtension(elems: List<ItemConfig>) : ConfigExtension<ItemConfig>(elems)
