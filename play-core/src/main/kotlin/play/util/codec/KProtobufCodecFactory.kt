package play.util.codec

object KProtobufCodecFactory : CodecFactory {

  override fun <T : Any> newCodec(type: Class<T>): Codec<T> {
    return KProtobufCodec(type.kotlin)
  }
}
