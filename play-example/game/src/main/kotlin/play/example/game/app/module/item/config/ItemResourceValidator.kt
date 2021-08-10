package play.example.game.app.module.item.config

import play.res.BasicResourceSet
import play.res.GenericResourceValidator
import play.res.ResourceSetSupplier

/**
 *
 * @author LiangZengle
 */
class ItemResourceValidator : GenericResourceValidator<ItemResource>() {
  override fun validate(
    configSet: BasicResourceSet<ItemResource>,
    resourceSetSupplier: ResourceSetSupplier,
    errors: MutableCollection<String>
  ) {
    if (configSet.size() < 2) {
      errors += "test config validator failure"
    }
  }
}
