package play

/**
 * Created by LiangZengle on 2020/2/16.
 */
interface ApplicationLoader {

  fun load(ctx: Context): Application

  data class Context(
    val conf: Configuration,
    val mode: Mode,
    val classScanner: ClassScanner,
    val lifecycle: ApplicationLifecycle
  )
}
