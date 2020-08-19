package play.example.module.mail.entity

import play.example.module.reward.model.Reward
import play.util.time.currentMillis

class Mail(
  var id: Int,
  val title: String,
  val content: String,
  val rewards: List<Reward>,
  val source: Int,
  var status: Int,
  val createTime: Long
) {

  fun setRead() {
    status = status or 1
  }

  fun isRead() = (status and 1) != 0

  fun setRewarded() {
    status = status or 2
  }

  fun isRewarded() = (status and 2) != 0

  fun hasReward() = !isRewarded() && rewards.isNotEmpty()
  override fun toString(): String {
    return "Mail(title='$title', content='$content', rewards=$rewards, status=$status)"
  }
}

data class MailBuilder(
  val title: String,
  val content: String,
  val rewards: List<Reward>,
  val source: Int,
  val createTime: Long = currentMillis()
)
