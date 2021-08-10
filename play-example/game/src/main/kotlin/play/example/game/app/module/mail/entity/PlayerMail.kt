package play.example.game.app.module.mail.entity

import play.example.game.app.module.player.entity.AbstractPlayerLongIdEntity

class PlayerMail(id: Long) : AbstractPlayerLongIdEntity(id) {

  private val mails = HashMap<Int, Mail>()

  fun addMail(mail: Mail) {
    require(mail.id == 0)
    // TODO
  }

  fun count() = mails.size
}
