package play.akka

import org.springframework.context.annotation.Import

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(AkkaConfiguration::class)
annotation class EnableAkka
