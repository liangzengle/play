package play.event

import org.springframework.context.annotation.Import

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(EventBusConfiguration::class)
annotation class EnableEventBus

