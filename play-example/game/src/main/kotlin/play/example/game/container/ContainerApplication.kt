package play.example.game.container

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.PlayCoreConfiguration
import play.akka.EnableAkka
import play.db.PlayDBConfiguration
import play.example.common.net.NettyServerConfiguration
import play.http.EnableHttpClient
import play.mongodb.PlayMongoClientConfiguration

/**
 *
 * @author LiangZengle
 */
@SpringBootApplication
@Configuration(proxyBeanMethods = false)
@EnableHttpClient
@EnableAkka
@Import(
  value = [
    PlayCoreConfiguration::class,
    PlayDBConfiguration::class,
    PlayMongoClientConfiguration::class,
    NettyServerConfiguration::class
  ]
)
class ContainerApplication
