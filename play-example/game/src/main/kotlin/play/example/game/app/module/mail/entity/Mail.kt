package play.example.game.app.module.mail.entity

import play.example.game.app.module.reward.model.Reward
import play.util.time.Time.currentMillis

data class Mail(
  val title: String,
  val content: String,
  val rewards: List<Reward>,
  val logSource: Int,
  val createTime: Long = currentMillis()
)
