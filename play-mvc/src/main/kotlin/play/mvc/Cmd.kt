package play.mvc

/**
 * 用于在[AbstractController]中定义接口
 *
 * @param value 接口id
 * @param dummy 是否仅做展示用，实际不处理请求，默认为false
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class Cmd(val value: Byte, val dummy: Boolean = false)
