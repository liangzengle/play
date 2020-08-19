package play.example.game.container.command

import com.fasterxml.jackson.core.JsonProcessingException
import play.util.json.Json
import java.lang.reflect.Parameter

sealed class ParameterConverter<out T : Any> {
  abstract fun convert(parameter: Parameter, arg: String): T

  companion object {
    operator fun invoke(parameterType: Class<*>): ParameterConverter<*> {
      return when (parameterType) {
        Boolean::class.java -> OfBoolean
        Int::class.java -> OfInt
        Long::class.java -> OfLong
        String::class.java -> OfString
        else -> OfJson
      }
    }
  }

  private object OfBoolean : ParameterConverter<Boolean>() {
    override fun convert(parameter: Parameter, arg: String): Boolean {
      return arg == "1" || arg == "true"
    }
  }

  private object OfInt : ParameterConverter<Int>() {
    override fun convert(parameter: Parameter, arg: String): Int {
      return try {
        arg.toInt()
      } catch (e: NumberFormatException) {
        throw IllegalArgumentException(e)
      }
    }
  }

  private object OfLong : ParameterConverter<Long>() {
    override fun convert(parameter: Parameter, arg: String): Long {
      return try {
        arg.toLong()
      } catch (e: NumberFormatException) {
        throw IllegalArgumentException(e)
      }
    }
  }

  private object OfString : ParameterConverter<String>() {
    override fun convert(parameter: Parameter, arg: String): String = arg
  }

  private object OfJson : ParameterConverter<Any>() {
    override fun convert(parameter: Parameter, arg: String): Any {
      return try {
        Json.toObject<Any>(arg, parameter.parameterizedType)
      } catch (e: JsonProcessingException) {
        throw IllegalArgumentException(e)
      }
    }
  }
}
