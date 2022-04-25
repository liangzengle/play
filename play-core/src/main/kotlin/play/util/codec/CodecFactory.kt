package play.util.codec

import play.Log
import play.util.reflect.Reflect

interface CodecFactory {
  fun <T : Any> newCodec(type: Class<T>): Codec<T>

  @Suppress("UNCHECKED_CAST")
  companion object {
    private val default: CodecFactory =
      System.getProperty("play.codec.factory").let { Reflect.getKotlinObjectOrNewInstance(it) }
        ?: KProtobufCodecFactory

    init {
      Log.debug("CodecFactory: ${default.javaClass.simpleName}")
    }

    @JvmStatic
    fun default(): CodecFactory = default
  }
}
