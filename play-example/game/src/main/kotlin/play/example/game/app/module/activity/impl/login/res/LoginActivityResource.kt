package play.example.game.app.module.activity.impl.login.res

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import play.example.game.app.module.reward.model.RewardList
import play.res.AbstractResource
import play.res.ResourceSetProvider

/**
 *
 * @author LiangZengle
 */
class LoginActivityResource : AbstractResource() {
  @Max(63)
  override val id: Int = 0

  @Valid
  val rewards = RewardList.Empty

  override fun initialize(resourceSetProvider: ResourceSetProvider, errors: MutableCollection<String>) {
    super.initialize(resourceSetProvider, errors)
  }
}
