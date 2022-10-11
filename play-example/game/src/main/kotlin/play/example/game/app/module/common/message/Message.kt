package play.example.game.app.module.common.message

import play.example.game.app.module.common.model.I18nText
import play.example.module.common.message.*

fun I18nText.toProto(): I18nTextProto {
  return when (this) {
    is I18nText.PlainText -> I18nTextProto(plainText = this.text)
    is I18nText.TemplateText -> I18nTextProto(tplText = TemplateTextProto(this.tplId, this.args.map { it.toProto() }))
  }
}

fun I18nText.Arg.toProto(): ArgProto {
  return when (this) {
    is I18nText.Arg.NumArg -> ArgProto(num = NumArgProto(this.num))
    is I18nText.Arg.TextArg -> ArgProto(text = TextArgProto(this.text))
    is I18nText.Arg.IdArg -> ArgProto(id = IdArgProto(this.id, IdArgType.fromValue(this.type) ?: IdArgType.none))
  }
}
