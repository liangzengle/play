package play.example.game.app.module.mail.entity

import play.example.game.app.module.common.model.I18nText
import play.example.game.app.module.reward.model.Reward
import play.example.game.app.module.reward.model.RewardList
import play.util.time.Time

data class Mail(
  val title: I18nText,
  val content: I18nText,
  val rewards: RewardList,
  val logSource: Int,
  val createTime: Long,
  val displayTime: Long
) {

  constructor(title: I18nText, content: I18nText) : this(title, content, RewardList.Empty, 0)

  constructor(title: I18nText, content: I18nText, rewards: RewardList, logSource: Int) : this(
    title, content, rewards, logSource, Time.currentMillis()
  )

  constructor(title: I18nText, content: I18nText, rewards: RewardList, logSource: Int, createTime: Long) : this(
    title, content, rewards, logSource, createTime, createTime
  )

  companion object {
    inline operator fun invoke(f: MailBuilder.() -> Unit): Mail {
      val builder = MailBuilder()
      f(builder)
      return builder.build()
    }
  }
}

class MailBuilder {
  private var title: I18nText? = null
  private var content: I18nText? = null
  private var rewards: RewardList = RewardList.Empty
  private var logSource: Int = 0
  private var createTime: Long = 0
  private var displayTime: Long = 0

  fun title(text: String): MailBuilder {
    title = I18nText(text)
    return this
  }

  fun title(titleId: Int): MailBuilder {
    title = I18nText(titleId)
    return this
  }

  fun title(titleId: Int, args: List<I18nText.Arg>): MailBuilder {
    title = I18nText(titleId, args)
    return this
  }

  fun content(text: String): MailBuilder {
    content = I18nText(text)
    return this
  }

  fun content(contentId: Int): MailBuilder {
    content = I18nText(contentId)
    return this
  }

  fun content(contentId: Int, args: List<I18nText.Arg>): MailBuilder {
    content = I18nText(contentId, args)
    return this
  }

  fun rewards(rewards: List<Reward>, logSource: Int): MailBuilder {
    this.rewards = RewardList(rewards)
    this.logSource = logSource
    return this
  }

  fun rewards(rewards: RewardList, logSource: Int): MailBuilder {
    this.rewards = rewards
    this.logSource = logSource
    return this
  }

  fun createTime(time: Long): MailBuilder {
    this.createTime = time
    return this
  }

  fun displayTime(time: Long): MailBuilder {
    this.displayTime = time
    return this
  }

  fun build(): Mail {
    val ctime = if (createTime == 0L) Time.currentMillis() else createTime
    val dtime = if (displayTime == 0L) ctime else displayTime
    return Mail(title!!, content!!, rewards, logSource, ctime, dtime)
  }
}
