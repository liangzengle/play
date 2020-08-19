package play.mvc

/**
 * Message编码/解码器
 * @author LiangZengle
 */
interface MessageCodec {

  fun encode(message: Message): ByteArray

  fun decode(bytes: ByteArray): Message
}
