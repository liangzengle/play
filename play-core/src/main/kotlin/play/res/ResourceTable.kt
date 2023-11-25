package play.res

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ResourceTable(val tableId: Int, val filePath: String)
