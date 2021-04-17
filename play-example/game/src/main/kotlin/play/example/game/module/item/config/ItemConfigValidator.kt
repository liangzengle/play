package play.example.game.module.item.config

import play.config.BasicConfigSet
import play.config.ConfigSetSupplier
import play.config.GenericConfigValidator
import javax.inject.Singleton

/**
 *
 * @author LiangZengle
 */
@Singleton
class ItemConfigValidator : GenericConfigValidator<ItemConfig>() {
  override fun validate(
    configSet: BasicConfigSet<ItemConfig>,
    configSetSupplier: ConfigSetSupplier,
    errors: MutableCollection<String>
  ) {
    if (configSet.size() < 2) {
      errors += "test config validator failure"
    }
  }
}
