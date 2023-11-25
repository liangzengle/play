package play.res

abstract class AbstractResource(@JvmField val id: Int, @JvmField val name: String, @JvmField val TID: String) {
  constructor() : this(0, "", "")
}
