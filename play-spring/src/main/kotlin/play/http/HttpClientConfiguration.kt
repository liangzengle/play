package play.http

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.httpclient.LoggingOnErrorHttpClient
import play.httpclient.PlayHttpClient
import play.httpclient.JHttpClient
import play.httpclient.JdkHttpClient
import java.net.http.HttpClient
import java.time.Duration


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(HttpClientConfiguration::class)
annotation class EnableHttpClient

@Configuration(proxyBeanMethods = false)
class HttpClientConfiguration {

  @Bean
  @ConditionalOnMissingBean
  fun jdkHttpClient(): PlayHttpClient {
    val client = JHttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(2))
      .build()
    return LoggingOnErrorHttpClient(JdkHttpClient(client))
  }
}
