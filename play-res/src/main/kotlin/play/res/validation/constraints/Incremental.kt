package play.res.validation.constraints

/**
 * 表示该字段是顺序递增的
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Incremental
