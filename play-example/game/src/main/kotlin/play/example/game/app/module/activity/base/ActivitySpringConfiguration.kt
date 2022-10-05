package play.example.game.app.module.activity.base

import akka.actor.typed.ActorRef
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.example.game.container.gs.GameServerScopeConfiguration
import play.spring.SingletonBeanContext
import play.util.classOf

@Configuration(proxyBeanMethods = false)
class ActivitySpringConfiguration : GameServerScopeConfiguration() {

  @Bean
  fun activityManager(beanContext: SingletonBeanContext): ActorRef<ActivityManager.Command> {
    return spawn("ActivityManager", classOf()) { mdc -> ActivityManager.create(mdc, beanContext) }
  }
}
