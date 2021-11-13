package play.http

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.net.http.HttpClient
import play.net.http.JHttpClient
import play.util.concurrent.CommonPool
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version
import java.time.Duration
import java.util.concurrent.Executor

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(HttpClientConfiguration::class)
annotation class EnableHttpClient

@Configuration(proxyBeanMethods = false)
class HttpClientConfiguration {

  @Bean
  @ConditionalOnMissingBean
  fun httpClient(@Autowired(required = false) @Qualifier("httpExecutor") executor: Executor?): HttpClient {
    val javaHttpClient = JHttpClient
      .newBuilder()
      .version(Version.HTTP_1_1)
      .followRedirects(Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(2))
//      .authenticator(Authenticator.getDefault())
      .apply {
        executor?.also(::executor)
      }
      .build()
    return HttpClient("default", javaHttpClient, Duration.ofSeconds(3))
  }

  @Bean("httpExecutor")
  @ConditionalOnMissingBean(name = ["httpExecutor"])
  fun httpExecutor(): Executor {
    return CommonPool
  }
}
