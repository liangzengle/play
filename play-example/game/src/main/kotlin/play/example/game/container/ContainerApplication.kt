package play.example.game.container

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.PlayCoreConfiguration
import play.akka.EnableAkka
import play.db.PlayDBConfiguration
import play.hotswap.InstallHotSwap
import play.http.EnableHttpClient
import play.mongodb.PlayMongoClientConfiguration
import play.netty.NettyConfiguration

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Configuration(proxyBeanMethods = false)
@EnableHttpClient
@EnableAkka
@InstallHotSwap
@Import(
  value = [
    PlayCoreConfiguration::class,
    PlayDBConfiguration::class,
    PlayMongoClientConfiguration::class,
    NettyConfiguration::class
  ]
)
class ContainerApplication
