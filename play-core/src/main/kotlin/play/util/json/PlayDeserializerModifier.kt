package play.util.json

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
class PlayDeserializerModifier : BeanDeserializerModifier() {

  override fun modifyDeserializer(
    config: DeserializationConfig?,
    beanDesc: BeanDescription,
    deserializer: JsonDeserializer<*>
  ): JsonDeserializer<*> {
    return when (beanDesc.beanClass) {
      Duration::class.java -> DurationDeserializer
      else -> deserializer
    }
  }
}
