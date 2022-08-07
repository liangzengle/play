package play.rsocket.serializer.kryo

interface PlayKryoCustomizer {
  fun customize(kryo: PlayKryo)
}
