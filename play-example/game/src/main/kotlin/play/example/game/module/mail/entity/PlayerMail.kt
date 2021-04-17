package play.example.game.module.mail.entity

import play.example.game.module.player.entity.AbstractPlayerEntity

class PlayerMail(id: Long) : AbstractPlayerEntity(id) {

  private val mails = HashMap<Int, Mail>()

  fun addMail(mail: Mail) {
    require(mail.id == 0)
    // TODO
  }

  fun count() = mails.size
}
