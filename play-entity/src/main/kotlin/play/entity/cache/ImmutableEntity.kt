package play.entity.cache

/**
 * 表示该实体类在创建之后就不会再被修改了, 将不会执行定时入库操作，减少不必要的写数据库
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ImmutableEntity
