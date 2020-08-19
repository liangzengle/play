package play.config

import kotlin.reflect.KClass

/**
 * 用于定义配置的相对路径
 *
 * @param value 配置根目录下的相对路径, 不带文件名称后缀
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ConfigPath(val value: String)

/**
 * 表示忽略该配置类
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Ignore

/**
 * 表示该配置不能为空
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class NoneEmpty

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
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class SingletonConfig

/**
 * 表示该配置类对应的是classpath中的文件
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Resource(val value: String)

/**
 * 表示id与[[table]]表的id完全一致
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Join(val table: KClass<out AbstractConfig>)

/**
 * 表示id与[[table]]表的分组完全一致
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class JoinGroup(val table: KClass<out AbstractConfig>)

