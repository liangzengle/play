package play.rsocket

import com.typesafe.config.Config
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import play.kryo.KryoSettings
import play.kryo.PlayKryoFactory
import play.rsocket.serializer.ByteBufToIOStreamAdapter
import play.rsocket.serializer.RSocketCodec
import play.rsocket.serializer.RSocketSerializerProvider
import play.rsocket.serializer.kryo.KryoSerializerProvider
import play.rsocket.serializer.kryo.io.ByteBufToInputOutput

/**
 *
 *
 * @author LiangZengle
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "play.rsocket", name = ["serializer"], havingValue = "kryo", matchIfMissing = true)
class PlayRSocketSerializerAutoConfiguration {
  @Bean
  fun ioStreamAdapter(): ByteBufToInputOutput {
    return ByteBufToInputOutput
  }

  @Bean
  fun serializerProvider(factory: PlayKryoFactory): KryoSerializerProvider {
    return KryoSerializerProvider(factory)
  }

  @Bean
  fun kryoFactory(settings: KryoSettings): PlayKryoFactory {
    return PlayKryoFactory(settings)
  }

  @Bean
  @ConditionalOnMissingBean
  fun kryoSettings(config: Config): KryoSettings {
    return KryoSettings(config.getConfig(KryoSettings.KEY))
  }

  @Bean
  @ConditionalOnMissingBean
  fun rsocketCodec(
    serializerProvider: RSocketSerializerProvider, ioStreamAdapter: ByteBufToIOStreamAdapter
  ): RSocketCodec {
    return RSocketCodec(serializerProvider, ioStreamAdapter)
  }
}
