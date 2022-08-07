package play.rsocket.serializer.kryo

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

internal class PlayKryoTest {
  private val kryo = PlayKryo.newInstance()
  private val serializer = KryoSerializer(kryo)

  init {
    kryo.addDefaultSerializerFactory(Collection::class.java, CollectionSerializer2<Collection<*>>())
    kryo.addDefaultSerializerFactory(Map::class.java, MapSerializer2<Map<*, *>>())

    kryo.addDefaultSerializerResolver(Collection::class.java, CollectionSerializerResolver())
    kryo.addDefaultSerializerResolver(Map::class.java, MapSerializerResolver())
  }

  @Test
  fun test() {
    val p1 = true
    val p2 = Byte.MAX_VALUE
    val p3 = Short.MAX_VALUE
    val p4 = Int.MAX_VALUE
    val p5 = Long.MAX_VALUE
    val p6 = Float.MAX_VALUE
    val p7 = Double.MAX_VALUE
    val p8 = "Hello"
    val p9 = booleanArrayOf(true)
    val p10 = byteArrayOf(1)
    val p11 = shortArrayOf(1)
    val p12 = intArrayOf(1)
    val p13 = longArrayOf(1)
    val p14 = floatArrayOf(1f)
    val p15 = doubleArrayOf(1.0)
    val p16 = listOf(1)
    val p17 = mapOf(1 to 1)
    val p18 = listOf(listOf(1))
    val p19 = mapOf(mapOf(1 to 1) to mapOf(1 to 1))


    val output = Output(1024, -1)
    serializer.writeBoolean(output, p1)
    serializer.writeByte(output, p2)
    serializer.writeShort(output, p3)
    serializer.writeInt(output, p4)
    serializer.writeLong(output, p5)
    serializer.writeFloat(output, p6)
    serializer.writeDouble(output, p7)
    serializer.writeUtf8(output, p8)
    serializer.writeBooleans(output, p9)
    serializer.writeBytes(output, p10)
    serializer.writeShorts(output, p11)
    serializer.writeInts(output, p12)
    serializer.writeLongs(output, p13)
    serializer.writeFloats(output, p14)
    serializer.writeDoubles(output, p15)
    serializer.writeObject(output, typeOf<List<Int>>().javaType, p16)
    serializer.writeObject(output, typeOf<Map<Int, Int>>().javaType, p17)
    serializer.writeObject(output, typeOf<List<List<Int>>>().javaType, p18)
    serializer.writeObject(output, typeOf<Map<Map<Int, Int>, Map<Int, Int>>>().javaType, p19)

    val input = Input(output.toBytes())
    val v1 = serializer.readBoolean(input)
    val v2 = serializer.readByte(input)
    val v3 = serializer.readShort(input)
    val v4 = serializer.readInt(input)
    val v5 = serializer.readLong(input)
    val v6 = serializer.readFloat(input)
    val v7 = serializer.readDouble(input)
    val v8 = serializer.readUtf8(input)
    val v9 = serializer.readBooleans(input)
    val v10 = serializer.readBytes(input)
    val v11 = serializer.readShorts(input)
    val v12 = serializer.readInts(input)
    val v13 = serializer.readLongs(input)
    val v14 = serializer.readFloats(input)
    val v15 = serializer.readDoubles(input)
    val v16 = serializer.readObject<Any>(input, typeOf<List<Int>>().javaType)
    val v17 = serializer.readObject<Any>(input, typeOf<Map<Int, Int>>().javaType)
    val v18 = serializer.readObject<Any>(input, typeOf<List<List<Int>>>().javaType)
    val v19 = serializer.readObject<Any>(input, typeOf<Map<Map<Int, Int>, Map<Int, Int>>>().javaType)


    assertThat(p1).isEqualTo(v1)
    assertThat(p2).isEqualTo(v2)
    assertThat(p3).isEqualTo(v3)
    assertThat(p4).isEqualTo(v4)
    assertThat(p5).isEqualTo(v5)
    assertThat(p6).isEqualTo(v6)
    assertThat(p7).isEqualTo(v7)
    assertThat(p8).isEqualTo(v8)
    assertThat(p9).isEqualTo(v9)
    assertThat(p10).isEqualTo(v10)
    assertThat(p11).isEqualTo(v11)
    assertThat(p12).isEqualTo(v12)
    assertThat(p13).isEqualTo(v13)
    assertThat(p14).isEqualTo(v14)
    assertThat(p15).isEqualTo(v15)
    assertThat(p16).isEqualTo(v16)
    assertThat(p17).isEqualTo(v17)
    assertThat(p18).isEqualTo(v18)
    assertThat(p19).isEqualTo(v19)

  }
}
