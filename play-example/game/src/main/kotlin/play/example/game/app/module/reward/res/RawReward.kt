package play.example.game.app.module.reward.res

import jakarta.validation.constraints.Positive
import play.example.game.app.module.reward.model.Reward
import play.util.el.Eval

data class RawReward(@field:Positive val id: Int, val num: String) {
  private val count: Long = try {
    if (num.isNotEmpty() && Character.isDigit(num[0])) {
      val value = num.toLong()
      require(value >= 0) { "num >= 0" }
      value
    } else {
      -1
    }
  } catch (e: NumberFormatException) {
    -1
  }

  fun getCount(args: Map<String, Any>): Long = if (count >= 0) count else Eval.eval(num, args).getLongOrThrow()

  fun toReward(args: Map<String, Any>): Reward = Reward(id, getCount(args))
}
