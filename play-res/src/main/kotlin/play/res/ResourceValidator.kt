package play.res

import play.util.reflect.Reflect

/**
 * 配置校验器，不支持依赖注入
 */
abstract class ResourceValidator {

  abstract fun validate(resourceSetSupplier: ResourceSetSupplier, errors: MutableCollection<String>)
}

abstract class GenericResourceValidator<T : AbstractResource> : ResourceValidator() {

  internal val resourceClass: Class<T> = Reflect.getRawClass(
    Reflect.getTypeArg(javaClass, GenericResourceValidator::class.java, 0)
  )

  final override fun validate(resourceSetSupplier: ResourceSetSupplier, errors: MutableCollection<String>) {
    validate(resourceSetSupplier.get(resourceClass), resourceSetSupplier, errors)
  }

  abstract fun validate(
    resourceSet: ResourceSet<T>,
    resourceSetSupplier: ResourceSetSupplier,
    errors: MutableCollection<String>
  )
}
