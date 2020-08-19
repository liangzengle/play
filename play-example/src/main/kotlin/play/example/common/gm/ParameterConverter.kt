package play.example.common.gm

import com.fasterxml.jackson.core.JsonProcessingException
import io.vavr.control.Option
import io.vavr.kotlin.none
import io.vavr.kotlin.some
import play.util.json.Json
import play.util.unsafeCast
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType

sealed class ParameterConverter<out T : Any> {
  abstract fun convert(parameter: Parameter, arg: String): T

  companion object {
    operator fun invoke(parameterType: Class<*>): ParameterConverter<*> {
      return when (parameterType) {
        Int::class.java -> OfInt
        Long::class.java -> OfLong
        String::class.java -> OfString
        Option::class.java -> OfOption
        else -> OfJson
      }
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

  private object OfOption : ParameterConverter<Option<*>>() {
    override fun convert(parameter: Parameter, arg: String): Option<*> {
      if (arg.isNotEmpty()) return none<Any>()
      val pType =
        parameter.parameterizedType.unsafeCast<ParameterizedType>().actualTypeArguments[0].unsafeCast<Class<*>>();
      return some(ParameterConverter(pType).convert(parameter, arg))
    }
  }


}
