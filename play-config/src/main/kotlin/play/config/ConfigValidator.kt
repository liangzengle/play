package play.config

import play.util.reflect.Reflect

interface ConfigValidator {

  fun validate(configSetSupplier: ConfigSetSupplier, errors: MutableList<String>)
}

abstract class GenericConfigValidator<T : AbstractConfig> : ConfigValidator {

  internal val configClass: Class<T> = Reflect.getRawClass(
    Reflect.getTypeArg(javaClass, GenericConfigValidator::class.java, 0)
  )

  final override fun validate(configSetSupplier: ConfigSetSupplier, errors: MutableList<String>) {
    validate(configSetSupplier.get(configClass), configSetSupplier, errors)
  }

  abstract fun validate(configSet: BasicConfigSet<T>, configSetSupplier: ConfigSetSupplier, errors: MutableList<String>)
}
