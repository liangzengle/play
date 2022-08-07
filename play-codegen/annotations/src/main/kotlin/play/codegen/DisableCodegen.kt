package play.codegen

/**
 * 禁用该类的代码生成
 * @author LiangZengle
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class DisableCodegen
