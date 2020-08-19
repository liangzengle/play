package play.example.module.mail.entity

import play.example.module.player.entity.PlayerEntity

class PlayerMail(id: Long) : PlayerEntity(id) {

  private val mails = HashMap<Int, Mail>()

  fun addMail(mail: Mail) {
    require(mail.id == 0)
    // TODO
  }

  fun count() = mails.size

}
