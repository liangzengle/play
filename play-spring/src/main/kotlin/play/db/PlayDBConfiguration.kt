package play.db

import com.typesafe.config.Config
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
class PlayDBConfiguration {

  @Bean
  @ConditionalOnMissingBean
  fun tableNameFormatter(): TableNameFormatter {
    return LowerUnderscoreFormatter()
  }

  @Bean
  @ConditionalOnMissingBean
  fun tableNameResolver(config: Config, tableNameFormatter: TableNameFormatter): TableNameResolver {
    val postfixes = config.getStringList("play.db.table-name-trim-postfixes")
    return TableNameResolver(postfixes, tableNameFormatter)
  }
}
