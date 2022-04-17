package play.example.game.container.command

import org.springframework.stereotype.Component

/**
 * 将方法标记为gm指令
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Command(val name: String = "", val desc: String)

/**
 * gm指令参数
 *
 * @property desc 参数描述
 * @property defaultValue 参数默认值
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param(val desc: String, val defaultValue: String = "")


/**
 * gm指令模块
 *
 * @property name 模块名
 * @property label 模块标签
 * @constructor
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Component
annotation class CommandModule(val name: String, val label: String)
