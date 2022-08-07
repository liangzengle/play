package play.mvc

import play.codec.MessageCodec

/**
 *
 * @author LiangZengle
 */
class KProtobufRequestBodyFactory : RequestBodyFactory {
  override fun empty(): RequestBody {
    return KProtobufRequestBody()
  }

  override fun builder(): RequestBodyBuilder {
    return KProtobufRequestBodyBuilder()
  }

  override fun parseFrom(bytes: ByteArray): RequestBody {
    return MessageCodec.decode<KProtobufRequestBody>(bytes)
  }
}
