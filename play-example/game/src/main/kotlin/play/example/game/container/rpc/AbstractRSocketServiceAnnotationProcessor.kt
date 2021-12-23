package play.example.game.container.rpc

import com.alibaba.rsocket.RSocketService
import com.alibaba.rsocket.spring.SpringRSocketService
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.annotation.AnnotationUtils

/**
 *
 * @author LiangZengle
 */
abstract class AbstractRSocketServiceAnnotationProcessor(
  private val defaultGroup: String,
  private val defaultVersion: String
) : BeanPostProcessor {

  @Throws(BeansException::class)
  final override fun postProcessBeforeInitialization(bean: Any, beanName: String?): Any {
    scanRSocketServiceAnnotation(bean, beanName)
    return bean
  }

  @Throws(BeansException::class)
  final override fun postProcessAfterInitialization(bean: Any, beanName: String?): Any {
    return bean
  }

  private fun scanRSocketServiceAnnotation(bean: Any, beanName: String?) {
    val managedBeanClass: Class<*> = bean.javaClass
    val reactiveService = AnnotationUtils.findAnnotation(
      managedBeanClass, RSocketService::class.java
    )
    reactiveService?.let { registerRSocketService(it, bean) }
    val springRSocketService = AnnotationUtils.findAnnotation(
      managedBeanClass, SpringRSocketService::class.java
    )
    springRSocketService?.let { registerRSocketService(it, bean) }
  }

  private fun registerRSocketService(rsocketServiceAnnotation: RSocketService, bean: Any) {
    val serviceName: String =
      rsocketServiceAnnotation.name.ifEmpty { rsocketServiceAnnotation.serviceInterface.java.canonicalName }
    val group = rsocketServiceAnnotation.group.ifEmpty { defaultGroup }
    val version = rsocketServiceAnnotation.version.ifEmpty { defaultVersion }
    addProvider(group, serviceName, version, rsocketServiceAnnotation.serviceInterface.java, bean)
  }

  private fun registerRSocketService(rsocketServiceAnnotation: SpringRSocketService, bean: Any) {
    val serviceInterface: Class<*> = rsocketServiceAnnotation.serviceInterface.java
    if (serviceInterface != Void::class.java) {
      var serviceName: String = rsocketServiceAnnotation.serviceInterface.java.canonicalName
      if (rsocketServiceAnnotation.value.isNotEmpty()) {
        serviceName = rsocketServiceAnnotation.value[0]
      }
      val group =
        rsocketServiceAnnotation.group.ifEmpty { defaultGroup }
      val version = rsocketServiceAnnotation.version.ifEmpty { defaultVersion }
      addProvider(group, serviceName, version, serviceInterface, bean)
    }
  }

  abstract fun addProvider(
    group: String, serviceName: String, version: String, serviceInterface: Class<*>, handler: Any
  )
}
