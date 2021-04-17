package play.example.common

/**
 * 将错误码、日志源修改为 getModuleId() * 1000 + x
 * @author LiangZengle
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ModularCode
