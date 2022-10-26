package play.hotswap

import org.springframework.context.annotation.Import

/**
 *
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(HotSwapConfiguration::class)
annotation class InstallHotSwap
