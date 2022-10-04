package play.example.game.app.module.currency.domain

import com.fasterxml.jackson.annotation.JsonValue
import play.codegen.EnumId
import play.codegen.EnumOps
import play.util.control.Result2
import play.util.enumeration.IdEnum

/**
 *
 * @author LiangZengle
 */
@EnumOps
enum class CurrencyType(@JsonValue @JvmField @EnumId val id: Int, val noEnoughErrorCode: Result2<Nothing>) : IdEnum {
  Gold(1, CurrencyStatusCode.GoldNotEnough),
  ;

  init {
    require(id == ordinal + 1)
  }

  override fun id(): Int {
    return id
  }
}
