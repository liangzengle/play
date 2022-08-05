package com.google.protobuf

import java.nio.ByteBuffer

/**
 *
 *
 * @author LiangZengle
 */
object ByteStrings {

  @JvmStatic
  fun wrap(bytes: ByteArray): ByteString = ByteString.wrap(bytes)

  @JvmStatic
  fun wrap(bytes: ByteArray, offset: Int, length: Int): ByteString = ByteString.wrap(bytes, offset, length)

  @JvmStatic
  fun wrap(buffer: ByteBuffer): ByteString = ByteString.wrap(buffer)
}
