package play.example.game.app.module.common.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import play.example.module.common.message.IdArgType

/**
 *
 *
 * @author LiangZengle
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
sealed class I18nText {
  data class PlainText(val text: String) : I18nText()
  data class TemplateText(val tplId: Int, val args: List<Arg>) : I18nText()

  companion object {
    @JvmStatic
    @JvmName("of")
    operator fun invoke(text: String) = PlainText(text)

    @JvmStatic
    @JvmName("of")
    @JvmOverloads
    operator fun invoke(tplId: Int, args: List<Arg> = emptyList()) = TemplateText(tplId, args)
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
  sealed class Arg {
    data class NumArg(val num: Long) : Arg()
    data class TextArg(val text: String) : Arg()
    data class IdArg(val id: Int, val type: Int) : Arg()

    companion object {
      @JvmStatic
      @JvmName("of")
      operator fun invoke(text: String) = TextArg(text)

      @JvmStatic
      @JvmName("of")
      operator fun invoke(num: Long) = NumArg(num)

      @JvmStatic
      @JvmName("of")
      operator fun invoke(id: Int, type: IdArgType) = IdArg(id, type.value)
    }
  }
}
