package play.res.validation.validator

import play.res.AbstractResource
import play.res.ResourceSet
import play.res.ResourceSetProvider
import play.res.validation.constraints.Unique
import play.util.collection.filterDuplicated
import play.util.reflect.Reflect

class UniqueFieldValidator : ResourceSetValidator() {
  override fun validate(
    resourceClass: Class<out AbstractResource>,
    resourceSet: ResourceSet<out AbstractResource>,
    resourceSetProvider: ResourceSetProvider,
    errors: MutableCollection<String>
  ) {
    Reflect.getAllFields(resourceClass) { it.isAnnotationPresent(Unique::class.java) }
      .asSequence()
      .flatMap { f ->
        sequenceOf(resourceSet.list().asSequence().map { Reflect.getFieldValue<Any>(f, it) }.filterDuplicated())
          .filter { it.isNotEmpty() }
          .map { "${resourceClass.simpleName}.${f.name}的值不能重复: $it" }
      }.forEach { errors.add(it) }
  }
}
