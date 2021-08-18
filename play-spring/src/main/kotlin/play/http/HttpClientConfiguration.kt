package play.http

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version
import java.time.Duration
import java.util.concurrent.ExecutorService

typealias JHttpClient = java.net.http.HttpClient

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(HttpClientConfiguration::class)
annotation class EnableHttpClient

@Configuration(proxyBeanMethods = false)
class HttpClientConfiguration {

  @Bean
  @ConditionalOnMissingBean
  fun httpClient(@Autowired(required = false) executorService: ExecutorService): HttpClient {
    val javaHttpClient = JHttpClient
      .newBuilder()
      .version(Version.HTTP_1_1)
      .followRedirects(Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(2))
//      .authenticator(Authenticator.getDefault())
      .executor(executorService)
      .build()
    return HttpClient(javaHttpClient, Duration.ofSeconds(3))
  }
}
