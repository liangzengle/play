package play.example.common.gm

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class GmCommand(val desc: String, val name: String = "")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class GmCommandArg(val desc: String, val defaultValue: String = "")
