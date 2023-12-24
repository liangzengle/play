package play.res

object Ref {

  class Just<T : AbstractResource> {
    internal lateinit var value: T

    @JvmName("get")
    operator fun invoke(): T = value
  }

  open class Maybe<T : AbstractResource> {
    internal var value: T? = null

    @JvmName("getOrNull")
    operator fun invoke(): T? = value

    fun isEmpty() = value === null
    fun isPresent() = value !== null
  }
}
