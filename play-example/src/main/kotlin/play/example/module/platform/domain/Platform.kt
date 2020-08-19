package play.example.module.platform.domain

import play.example.common.*

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

  companion object : IdEnumCompanion<Platform>, NamedEnumCompanion<Platform>, SizedEnumCompanion<Platform> {
    override val elems = values()

    // 需要先定义elems
    init {
      ensureUniqueId()
      ensureUniqueName()
    }
  }
}
