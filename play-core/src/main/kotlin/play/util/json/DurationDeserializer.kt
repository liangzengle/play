package play.util.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import play.time.Time
import java.time.DateTimeException
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
object DurationDeserializer : com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer() {

  override fun _fromString(parser: JsonParser?, ctxt: DeserializationContext?, value0: String?): Duration {
    return super._fromString(parser, ctxt, value0)
  }

  override fun <R : Any?> _handleDateTimeException(
    context: DeserializationContext?,
    e0: DateTimeException?,
    value: String?
  ): R {
    if (value != null) {
      try {
        @Suppress("UNCHECKED_CAST")
        return Time.parseDuration(value) as R
      } catch (e: Exception) {
        // ignore
      }
    }
    return super._handleDateTimeException(context, e0, value)
  }
}
