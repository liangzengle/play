package play.mvc

/**
 * 表示该接口或者该类中的所有接口不在玩家线程中执行
 * @author LiangZengle
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class NotPlayerThread
