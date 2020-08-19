package play.db

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class TableName(val value: String, val format: Boolean = true)
