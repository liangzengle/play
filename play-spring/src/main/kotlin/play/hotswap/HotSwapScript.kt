package play.hotswap

import org.springframework.beans.factory.InitializingBean

/**
 *
 *
 * @author LiangZengle
 */
abstract class HotSwapScript : InitializingBean {

  override fun afterPropertiesSet() {
    execute()
  }

  abstract fun execute()
}
