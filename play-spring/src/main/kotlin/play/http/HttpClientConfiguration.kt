package play.http

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import play.httpclient.KtorHttpClient
import play.httpclient.LoggingOnErrorHttpClient
import play.httpclient.NettyHttpClient
import play.httpclient.PlayHttpClient
import play.util.concurrent.CommonPool
import play.util.http.JHttpClient
import play.util.http.JdkHttpClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executor


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(HttpClientConfiguration::class)
annotation class EnableHttpClient

@Configuration(proxyBeanMethods = false)
class HttpClientConfiguration {

  @Bean
  @ConditionalOnClass(name = ["org.asynchttpclient.AsyncHttpClient"])
  fun asyncHttpClient(@Autowired(required = false) @Qualifier("httpExecutor") executor: Executor?): PlayHttpClient {
    return NettyHttpClient(executor ?: CommonPool)
  }

  @Bean
  @ConditionalOnClass(name = ["io.ktor.client.HttpClient"])
  fun ktorHttpClient(): PlayHttpClient {
    return LoggingOnErrorHttpClient(KtorHttpClient())
  }

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
