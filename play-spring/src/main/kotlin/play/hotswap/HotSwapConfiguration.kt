package play.hotswap

import com.typesafe.config.Config
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.SystemProps

/**
 *
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "play.hotswap", value = ["enabled"], havingValue = "true")
class HotSwapConfiguration {

  @Bean(initMethod = "start")
  fun hotSwapWatcher(config: Config): HotSwapWatcher {
    val scriptDir = config.getString("play.hotswap.class-dir")
    val agent = config.getString("play.hotswap.agent")
    SystemProps.set(HotSwapAgent.AGENT_PATH, agent)
    return HotSwapWatcher(scriptDir)
  }
}
