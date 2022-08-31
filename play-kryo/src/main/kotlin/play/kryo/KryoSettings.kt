package play.kryo

import com.typesafe.config.Config

/**
 *
 *
 * @author LiangZengle
 */
class KryoSettings(config: Config) {

  companion object {
    const val KEY = "play-kryo"
  }

  val serializerType: String = config.getString("type")

  // Each entry should be: FQCN -> integer id
  val classNameMappings: Map<String, String> =
    config.getConfig("mappings").root().unwrapped().mapValues { it.toString() }
  val classNames: List<String> = config.getStringList("classes")

  // Strategy: default, explicit, incremental, automatic
  val idStrategy: String = config.getString("id-strategy")
  val implicitRegistrationLogging: Boolean = config.getBoolean("implicit-registration-logging")

  val kryoReferenceMap: Boolean = config.getBoolean("kryo-reference-map")

  val resolveSubclasses: Boolean = config.getBoolean("resolve-subclasses")
}
