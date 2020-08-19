package play

/**
 * Created by LiangZengle on 2020/2/15.
 */
enum class Mode {
  Dev,
  Prod;

  companion object {
    @JvmStatic
    fun forName(name: String): Mode {
      return when (name.toLowerCase()) {
        "dev" -> Dev
        "prod" -> Prod
        else -> Dev
      }
    }
  }

  fun isDev() = this === Dev

  fun isProd() = this === Prod
  override fun toString(): String {
    return when (this) {
      Dev -> "dev"
      Prod -> "prod"
    }
  }
}
