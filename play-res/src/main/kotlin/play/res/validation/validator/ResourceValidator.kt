package play.res.validation.validator

import play.res.AbstractResource
import play.res.ResourceSet
import play.res.ResourceSetProvider
import play.util.isAbstract
import play.util.reflect.Reflect
import play.util.unsafeCast

/**
 * 配置校验器，不支持依赖注入
 */
abstract class ResourceValidator {

  abstract fun validate(resourceSetProvider: ResourceSetProvider, errors: MutableCollection<String>)
}

abstract class ResourceSetValidator : ResourceValidator() {

  override fun validate(resourceSetProvider: ResourceSetProvider, errors: MutableCollection<String>) {
    for ((resourceClass, resourceSet) in resourceSetProvider.resourceSetMap) {
      validate(resourceClass, resourceSet, resourceSetProvider, errors)
    }
  }

  abstract fun validate(
    resourceClass: Class<out AbstractResource>,
    resourceSet: ResourceSet<out AbstractResource>,
    resourceSetProvider: ResourceSetProvider,
    errors: MutableCollection<String>
  )
}

abstract class GenericResourceValidator<T : AbstractResource> : ResourceValidator() {

  internal val resourceClass: Class<T> =
    Reflect.getRawClassOfTypeArg(javaClass, GenericResourceValidator::class.java, 0)

  final override fun validate(resourceSetProvider: ResourceSetProvider, errors: MutableCollection<String>) {
    if (resourceClass.isAbstract()) {
      for ((resourceClass, resourceSet) in resourceSetProvider.resourceSetMap) {
        if (this.resourceClass.isAssignableFrom(resourceClass)) {
          validate(resourceSet.unsafeCast(), resourceSetProvider, errors)
        }
      }
    } else if (resourceSetProvider.contains(resourceClass)) {
      validate(resourceSetProvider.get(resourceClass), resourceSetProvider, errors)
    }
  }

  abstract fun validate(
    resourceSet: ResourceSet<T>,
    resourceSetProvider: ResourceSetProvider,
    errors: MutableCollection<String>
  )
}
