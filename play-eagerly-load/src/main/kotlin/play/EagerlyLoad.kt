package play

/**
 * 启动时加载的类
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class EagerlyLoad
