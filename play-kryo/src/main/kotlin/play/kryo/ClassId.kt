package play.kryo

/**
 *
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ClassId(val value: Int)
