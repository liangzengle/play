package play.entity

/**
 * 表示该实体类在创建之后就不会被修改了，
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ImmutableEntity
