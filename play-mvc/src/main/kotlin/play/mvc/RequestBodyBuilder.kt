package play.mvc

/**
 *
 *
 * @author LiangZengle
 */
interface RequestBodyBuilder {

  fun write(value: Boolean): RequestBodyBuilder

  fun write(value: Int): RequestBodyBuilder

  fun write(value: Long): RequestBodyBuilder

  fun write(value: String): RequestBodyBuilder

  fun write(array: IntArray): RequestBodyBuilder

  fun writeIntList(intList: List<Int>): RequestBodyBuilder

  fun writeLongList(longList: List<Long>): RequestBodyBuilder

  fun write(array: LongArray): RequestBodyBuilder

  fun write(array: ByteArray): RequestBodyBuilder

  fun write(obj: Any): RequestBodyBuilder

  fun build(): RequestBody
}
