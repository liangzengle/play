package play

enum class Mode {
  Dev {
    override fun isDev(): Boolean = true

    override fun isProd(): Boolean = false
  },
  Prod {
    override fun isDev(): Boolean = false

    override fun isProd(): Boolean = true
  };

  abstract fun isDev(): Boolean

  abstract fun isProd(): Boolean
}
