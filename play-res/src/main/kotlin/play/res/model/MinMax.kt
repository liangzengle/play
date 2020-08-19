package play.res.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.validation.constraints.AssertTrue

class MinMaxInt private constructor(@JvmField val min: Int, @JvmField val max: Int) {

  @JsonValue
  fun toArray() = intArrayOf(min, max)

  @AssertTrue(message = "`min` should not greater than `max`")
  private fun isValid(): Boolean {
    return min <= max
  }

  companion object {
    @JvmStatic
    val Zero = MinMaxInt(0, 0)

    @JvmName("of")
    @JvmStatic
    operator fun invoke(min: Int, max: Int): MinMaxInt {
      require(min <= max) { "min should not greater than max: min=$min, max=$max" }
      return MinMaxInt(min, max)
    }

    @JvmStatic
    @JsonCreator
    fun fromJson(jsonNode: JsonNode): MinMaxInt {
      return when (jsonNode) {
        is ArrayNode -> MinMaxInt(jsonNode.get(0).asInt(), jsonNode.get(1).asInt())
        is ObjectNode -> MinMaxInt(jsonNode.get("min").asInt(), jsonNode.get("max").asInt())
        else -> throw IllegalArgumentException("${jsonNode.javaClass.simpleName}$jsonNode")
      }
    }
  }
}

class MinMaxLong private constructor(@JvmField val min: Long, @JvmField val max: Long) {

  @JsonValue
  fun toArray() = longArrayOf(min, max)

  @AssertTrue(message = "`min` should not greater than `max`")
  private fun isValid(): Boolean {
    return min <= max
  }

  companion object {
    @JvmStatic
    val Zero = MinMaxLong(0L, 0L)

    @JvmName("of")
    @JvmStatic
    operator fun invoke(min: Long, max: Long): MinMaxLong {
      require(min <= max) { "min should not greater than max: min=$min, max=$max" }
      return MinMaxLong(min, max)
    }

    @JvmStatic
    @JsonCreator
    private fun fromJson(jsonNode: JsonNode): MinMaxLong {
      return when (jsonNode) {
        is ArrayNode -> MinMaxLong(jsonNode.get(0).asLong(), jsonNode.get(1).asLong())
        is ObjectNode -> MinMaxLong(jsonNode.get("min").asLong(), jsonNode.get("max").asLong())
        else -> throw IllegalArgumentException("${jsonNode.javaClass.simpleName}$jsonNode")
      }
    }
  }
}

class MinMaxDouble private constructor(@JvmField val min: Double, @JvmField val max: Double) {

  @JsonValue
  fun toArray() = doubleArrayOf(min, max)

  @AssertTrue(message = "`min` should not greater than `max`")
  private fun isValid(): Boolean {
    return min <= max
  }

  companion object {
    @JvmStatic
    val Zero = MinMaxDouble(0.0, 0.0)

    @JvmName("of")
    @JvmStatic
    operator fun invoke(min: Double, max: Double): MinMaxDouble {
      require(min <= max) { "min should not greater than max: min=$min, max=$max" }
      return MinMaxDouble(min, max)
    }

    @JvmStatic
    @JsonCreator
    private fun fromJson(jsonNode: JsonNode): MinMaxDouble {
      return when (jsonNode) {
        is ArrayNode -> MinMaxDouble(jsonNode.get(0).asDouble(), jsonNode.get(1).asDouble())
        is ObjectNode -> MinMaxDouble(jsonNode.get("min").asDouble(), jsonNode.get("max").asDouble())
        else -> throw IllegalArgumentException("${jsonNode.javaClass.simpleName}$jsonNode")
      }
    }
  }
}
