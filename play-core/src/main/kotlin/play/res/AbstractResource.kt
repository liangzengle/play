package play.res

abstract class AbstractResource(@JvmField val id: Int, @JvmField val name: String) {

  var disabled = false
    internal set

  var globalId = 0
    internal set

  var TID = ""
    internal set

  fun postConstruct() {}
}
