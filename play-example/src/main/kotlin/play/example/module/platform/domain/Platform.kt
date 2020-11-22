package play.example.module.platform.domain

import play.util.enumration.IdEnum
import play.util.enumration.IdNamedEnumOps
import play.util.enumration.NamedEnum
import play.util.enumration.idNamedEnumOpsOf

enum class Platform(override val id: Int, private val text: String) : IdEnum<Platform>, NamedEnum<Platform> {
  // 开发环境
  Dev(127, "dev"),
  // --------------------------------------

  ;

  init {
    require(id > 0 && id <= UByte.MAX_VALUE.toInt()) { "id(0~${Byte.MAX_VALUE}) overflow: $id" }
  }

  fun getId() = id.toByte()

  override fun getName(): String {
    return text
  }

  companion object : IdNamedEnumOps<Platform> by idNamedEnumOpsOf(values())
}
