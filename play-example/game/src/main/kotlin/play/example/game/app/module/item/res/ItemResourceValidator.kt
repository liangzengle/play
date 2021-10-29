package play.example.game.app.module.item.res

import play.res.ResourceSet
import play.res.ResourceSetProvider
import play.res.validation.validator.GenericResourceValidator

/**
 *
 * @author LiangZengle
 */
class ItemResourceValidator : GenericResourceValidator<ItemResource>() {
  override fun validate(
    resourceSet: ResourceSet<ItemResource>,
    resourceSetProvider: ResourceSetProvider,
    errors: MutableCollection<String>
  ) {
    if (resourceSet.size() < 2) {
      errors += "test config validator failure"
    }
  }
}
