package play.mvc

/**
 * 用于使用AnnotationProcessor为[AbstractController]生成接口调用类
 *
 * @param moduleId 模块id
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Controller(val moduleId: Short)

/**
 * 所有Controller的父类，定义各自的前端接口
 *
 * @param moduleId 模块id
 */
abstract class AbstractController(val moduleId: Short)
