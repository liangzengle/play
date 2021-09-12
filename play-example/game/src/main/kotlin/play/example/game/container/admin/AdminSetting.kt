package play.example.game.container.admin

import play.res.AbstractConfig
import play.res.ResourcePath
import play.res.ResourceSetSupplier

/**
 *
 * @author LiangZengle
 */
@ResourcePath("admin.conf")
data class AdminSetting(
  val whiteList: Set<String>,
  val playerCreateReportUrl: String
) : AbstractConfig() {

  override fun initialize(resourceSetSupplier: ResourceSetSupplier, errors: MutableCollection<String>) {
    println(this)
  }
}
