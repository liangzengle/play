package play.rsocket.serializer.kryo.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author LiangZengle
 */
class ByteBufOutputTest {


  @Test
  public void test() {
    int n = 100;
    byte[] bytes = new byte[n];
    for (int i = 0; i < n; i++) {
      bytes[i] = (byte) ThreadLocalRandom.current().nextInt(0, 128);
    }
    String value = new String(bytes);
    Assertions.assertArrayEquals(byteBuf_writeUtf8(value, 0, value.length()), string_getBytes(value, 0, value.length()));
    Assertions.assertArrayEquals(byteBuf_writeUtf8(value, 0, 10), string_getBytes(value, 0, 10));
    Assertions.assertArrayEquals(byteBuf_writeUtf8(value, 0, 55), string_getBytes(value, 0, 55));
    Assertions.assertArrayEquals(byteBuf_writeUtf8(value, 0, 88), string_getBytes(value, 0, 88));
    Assertions.assertArrayEquals(byteBuf_writeUtf8(value, 3, 88), string_getBytes(value, 3, 88));
  }

  private byte[] byteBuf_writeUtf8(String value, int start, int end) {
    ByteBuf buffer = Unpooled.buffer(end - start);
    ByteBufUtil.writeUtf8(buffer, value, start, end);
    return ByteBufUtil.getBytes(buffer);
  }

  @SuppressWarnings("deprecation")
  private byte[] string_getBytes(String value, int start, int end) {
    byte[] tmp = new byte[end - start];
    value.getBytes(start, end, tmp, 0);
    return tmp;
  }
}