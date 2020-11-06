package play.config

import play.util.reflect.Reflect

/**
 * 配置刷新监听器，
 * Created by LiangZengle on 2020/2/23.
 */
interface ConfigRefreshListener<Event : ConfigRefreshEvent> {
  @Throws(Exception::class)
  fun onEvent(event: Event)
}

abstract class GenericConfigRefreshListener<T : AbstractConfig, E : ConfigRefreshEvent> : ConfigRefreshListener<E> {

  internal val configClass: Class<T> =
    Reflect.getRawClass(Reflect.getTypeArg(javaClass, GenericConfigRefreshListener::class.java, 0))

}
