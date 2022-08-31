package play.kryo

import com.esotericsoftware.kryo.Kryo
import java.util.*

interface PlayKryoCustomizer {
  fun customize(kryo: Kryo)

  companion object {
    private val customizers = ServiceLoader.load(PlayKryoCustomizer::class.java).toList()

    fun customize(kryo: Kryo) {
      customizers.forEach { it.customize(kryo) }
    }
  }
}
