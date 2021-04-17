package play.config

import play.inject.guice.EnableMultiBinding
import play.util.reflect.Reflect

@EnableMultiBinding
interface ConfigValidator {

  fun validate(configSetSupplier: ConfigSetSupplier, errors: MutableCollection<String>)
}

abstract class GenericConfigValidator<T : AbstractConfig> : ConfigValidator {

  internal val configClass: Class<T> = Reflect.getRawClass(
    Reflect.getTypeArg(javaClass, GenericConfigValidator::class.java, 0)
  )

  final override fun validate(configSetSupplier: ConfigSetSupplier, errors: MutableCollection<String>) {
    validate(configSetSupplier.get(configClass), configSetSupplier, errors)
  }

  abstract fun validate(
    configSet: BasicConfigSet<T>,
    configSetSupplier: ConfigSetSupplier,
    errors: MutableCollection<String>
  )
}
