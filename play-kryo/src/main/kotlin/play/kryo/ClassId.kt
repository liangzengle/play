package play.kryo

import org.checkerframework.checker.index.qual.Positive

/**
 * Class id for kryo serialization, must be positive
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ClassId(val value: @Positive Int)
