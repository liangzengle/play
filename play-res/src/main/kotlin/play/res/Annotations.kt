package play.res

import kotlin.reflect.KClass

/**
 * 用于定义配置的相对路径
 *
 * @param value 配置根目录下的相对路径, 不带文件名称后缀
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ResourcePath(val value: String)

/**
 * 表示忽略该配置类
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Ignore

/**
 * 热更删除配置时不会报警告
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class AllowRemoveOnReload

/**
 * 表示该配置不能为空
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class NonEmpty

/**
 * 表示该配置不允许重加载
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class NotReloadable

/**
 * 指定该配置允许的最小id，默认最小id为1
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class MinID(val value: Int)

/**
 * 表示该字段不能重复
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Unique

/**
 * 表示该字段是顺序递增的
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Incremental

/**
 * 表示该配置有且只有1条数据
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class SingletonResource

/**
 * 表示id与[[table]]表的id完全一致
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Extend(val table: KClass<out AbstractResource>)
