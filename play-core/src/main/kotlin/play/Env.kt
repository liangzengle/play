package play

interface Env {

  fun mode(): Mode

  companion object {
    private var _current: Env = object : Env {
      override fun mode(): Mode {
        return Mode.Dev
      }
    }

    @JvmStatic
    fun setCurrent(env: Env) {
      _current = env
    }

    @JvmStatic
    fun current(): Env = _current
  }
}
