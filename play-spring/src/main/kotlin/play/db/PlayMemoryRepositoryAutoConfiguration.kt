package play.db

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.db.memory.MemoryRepository

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "play.repository", name = ["type"], havingValue = "memory")
class PlayMemoryRepositoryAutoConfiguration {

  @Bean
  fun memoryRepository(): MemoryRepository {
    return MemoryRepository()
  }
}
