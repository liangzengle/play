package play.spring

import org.springframework.boot.ApplicationContextFactory
import org.springframework.boot.WebApplicationType
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 *
 * @author LiangZengle
 */
class PlayNonWebApplicationContextFactory : ApplicationContextFactory {
  override fun create(webApplicationType: WebApplicationType): ConfigurableApplicationContext {
//    if (webApplicationType != WebApplicationType.NONE) {
//      throw IllegalArgumentException("WebApplication is not supported")
//    }
    try {
      return AnnotationConfigApplicationContext(PlayListableBeanFactory())
    } catch (ex: Exception) {
      throw IllegalStateException(
        "Unable create a default ApplicationContext instance, "
          + "you may need a custom ApplicationContextFactory", ex
      )
    }
  }
}
