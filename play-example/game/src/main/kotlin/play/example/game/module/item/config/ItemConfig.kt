package play.example.game.module.item.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import javax.validation.Valid
import play.config.*
import play.example.game.module.common.config.CommonSetting
import play.example.game.module.item.domain.ItemType
import play.example.game.module.reward.config.RawReward

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

  var bagFullMailId = 0
    private set

  override fun groupId(): ItemType = type

  override fun postInitialize(configSetSupplier: ConfigSetSupplier, errors: MutableCollection<String>) {
    println("ItemConfig postInitialize: $this")
    val commonSetting = configSetSupplier.getSingleton(CommonSetting::class.java)
    bagFullMailId = commonSetting.get().bagFullMailId
  }

  override fun toString(): String {
    return "ItemConfig($id, $name, $desc, $numberValue, $jsonValue, $rewards)"
  }
}

class ItemConfigExtension(elems: List<ItemConfig>) : ConfigExtension<ItemConfig>(elems)
