package play.example.game.app.module.item.res

import play.res.GenericResourceValidator
import play.res.ResourceSet
import play.res.ResourceSetSupplier

/**
 *
 * @author LiangZengle
 */
class ItemResourceValidator : GenericResourceValidator<ItemResource>() {
  override fun validate(
    resourceSet: ResourceSet<ItemResource>,
    resourceSetSupplier: ResourceSetSupplier,
    errors: MutableCollection<String>
  ) {
    if (resourceSet.size() < 2) {
      errors += "test config validator failure"
    }
  }
}
