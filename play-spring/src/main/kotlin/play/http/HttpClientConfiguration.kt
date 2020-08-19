package play.http

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.net.netty.http.NettyHttpClient
import play.util.concurrent.CommonPool
import play.util.http.PlayHttpClient
import java.util.concurrent.Executor

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(HttpClientConfiguration::class)
annotation class EnableHttpClient

@Configuration(proxyBeanMethods = false)
class HttpClientConfiguration {

  @Bean
  @ConditionalOnMissingBean
  fun httpClient(@Autowired(required = false) @Qualifier("httpExecutor") executor: Executor?): PlayHttpClient {
    return NettyHttpClient("default", executor ?: CommonPool)
  }

  @Bean("httpExecutor")
  @ConditionalOnMissingBean(name = ["httpExecutor"])
  fun httpExecutor(): Executor {
    return CommonPool
  }
}
