package play.res

class Ref<T : AbstractResource> {
  lateinit var value: T
    internal set
}
