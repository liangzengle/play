package play.res.validation.constraints

/**
 * 表示该字段不能重复
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Unique
