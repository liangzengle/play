package play.example.game.container.db

import com.mongodb.reactivestreams.client.MongoClient
import com.typesafe.config.Config
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import play.db.TableNameResolver

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class ContainerRepositoryConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "play", name = ["db.repository"], havingValue = "mongodb")
  fun containerMongoDBRepository(
    tableNameResolver: TableNameResolver, client: MongoClient, config: Config, env: Environment
  ): ContainerRepositoryProvider {
    return ContainerMongoDBRepositoryProvider(tableNameResolver, client, config)
  }

  @Bean
  @ConditionalOnProperty(prefix = "play", name = ["db.repository"], havingValue = "memory")
  fun containerMemoryRepository(): ContainerRepositoryProvider {
    return ContainerMemoryRepositoryProvider()
  }
}
