package play.db

import com.typesafe.config.Config
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.db.memory.MemoryRepository

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class PlayDBConfiguration {

  @Bean
  @ConditionalOnMissingBean(TableNameFormatter::class)
  fun tableNameFormatter(): TableNameFormatter {
    return LowerUnderscoreFormatter()
  }

  @Bean
  @ConditionalOnMissingBean(TableNameResolver::class)
  fun tableNameResolver(config: Config, tableNameFormatter: TableNameFormatter): TableNameResolver {
    val postfixes = config.getStringList("play.db.table-name-trim-postfixes")
    return TableNameResolver(postfixes, tableNameFormatter)
  }

  @Bean
  @ConditionalOnProperty(prefix = "play", name = ["play.db.repository"], havingValue = "memory")
  fun repository(): Repository {
    return MemoryRepository()
  }
}
