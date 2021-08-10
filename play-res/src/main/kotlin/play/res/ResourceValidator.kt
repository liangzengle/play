package play.res

import play.util.reflect.Reflect

/**
 * 配置校验器，不支持依赖注入
 */
abstract class ResourceValidator {

  abstract fun validate(resourceSetSupplier: ResourceSetSupplier, errors: MutableCollection<String>)
}

abstract class GenericResourceValidator<T : AbstractResource> : ResourceValidator() {

  internal val configClass: Class<T> = Reflect.getRawClass(
    Reflect.getTypeArg(javaClass, GenericResourceValidator::class.java, 0)
  )

  final override fun validate(resourceSetSupplier: ResourceSetSupplier, errors: MutableCollection<String>) {
    validate(resourceSetSupplier.get(configClass), resourceSetSupplier, errors)
  }

  abstract fun validate(
    configSet: BasicResourceSet<T>,
    resourceSetSupplier: ResourceSetSupplier,
    errors: MutableCollection<String>
  )
}
