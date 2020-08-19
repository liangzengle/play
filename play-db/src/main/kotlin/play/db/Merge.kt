package play.db

import java.lang.annotation.Inherited

/**
 * 合并规则
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Merge(val value: Strategy) {

  enum class Strategy {
    /**
     * 保留主服的数据
     */
    PRESERVE_PRIMARY,

    /**
     * 保留所有数据
     */
    All,

    /**
     * 全部清除
     */
    Clear
  }
}
