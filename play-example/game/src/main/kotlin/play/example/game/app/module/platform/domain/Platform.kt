package play.example.game.app.module.platform.domain

import play.codegen.EnumId
import play.codegen.EnumOps
import play.util.enumeration.IdEnum

@EnumOps
enum class Platform(@JvmField @field:EnumId val id: Int) : IdEnum {
  // 开发环境
  Dev(127),
  // --------------------------------------

  ;

  init {
    require(id > 0 && id <= UByte.MAX_VALUE.toInt()) { "id(0~${Byte.MAX_VALUE}) overflow: $id" }
  }

  override fun id(): Int {
    return id
  }

  fun toByte(): Byte {
    return id.toByte()
  }
}
