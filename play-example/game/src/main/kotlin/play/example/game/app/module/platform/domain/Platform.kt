package play.example.game.app.module.platform.domain

import play.util.enumration.IdEnum
import play.util.enumration.IdNamedEnumOps
import play.util.enumration.NamedEnum
import play.util.enumration.idNamedEnumOpsOf

enum class Platform(override val id: Int) : IdEnum<Platform>, NamedEnum<Platform> {
  // 开发环境
  Dev(127),
  // --------------------------------------

  ;

  init {
    require(id > 0 && id <= UByte.MAX_VALUE.toInt()) { "id(0~${Byte.MAX_VALUE}) overflow: $id" }
  }

  fun getId() = id.toByte()

  override fun getName(): String {
    return this.name
  }

  companion object : IdNamedEnumOps<Platform> by idNamedEnumOpsOf(values())
}
