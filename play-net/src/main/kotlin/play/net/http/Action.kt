package play.net.http

import com.google.common.collect.Maps

data class RoutePath(val root: String, val children: List<ParPath>) {

  fun isPlain() = children.isEmpty()

  fun hasVariable() = !isPlain()

  fun matches(path: String): Boolean {
    if (isPlain()) {
      return root == path
    }
    if (!path.startsWith(root)) {
      return false
    }
    if (path.length == root.length) {
      return false
    }
    val startIndex = root.length + 1
    val endIndex = if (path.last() == '/') path.length - 1 else path.length
    val tailPath = path.substring(startIndex, endIndex)
    val it1 = tailPath.splitToSequence('/').iterator()
    val it2 = children.iterator()
    while (true) {
      if (it1.hasNext() xor it2.hasNext()) {
        return false
      }
      if (!it1.hasNext()) {
        return true
      }
      val p1 = it1.next()
      val p2 = it2.next()
      if (!p2.isVariable && p1 != p2.name) {
        return false
      }
    }
  }

  fun extractPathParameters(path: String): Map<String, String> {
    if (isPlain()) return emptyMap()
    val result = Maps.newHashMapWithExpectedSize<String, String>(2)
    var startIndex = root.length + 1
    for (index in children.indices) {
      var nextIndex = path.indexOf('/', startIndex)
      if (nextIndex == -1) {
        nextIndex = path.length
      }
      val p = children[index]
      if (p.isVariable) {
        val value = path.substring(startIndex, nextIndex)
        result[p.name] = value
      }
      startIndex = nextIndex + 1
    }
    return result
  }

  override fun toString(): String {
    return if (isPlain()) {
      root
    } else {
      val b = StringBuilder()
      b.append(root)
      for (i in children.indices) {
        b.append('/')
        val (name, isVariable) = children[i]
        if (isVariable) {
          b.append('{')
        }
        b.append(name)
        if (isVariable) {
          b.append('}')
        }
      }
      b.toString()
    }
  }

  companion object {

    @JvmStatic
    fun parse(path: String): RoutePath {
      val idx = path.indexOf('{')
      try {
        return if (idx == -1) {
          RoutePath(path, emptyList())
        } else {
          val root = path.substring(0, idx - 1)
          val children = path.subSequence(idx, path.length)
            .splitToSequence('/')
            .map {
              if (it.startsWith('{')) {
                ParPath(it.substring(1, it.length - 1), true)
              } else ParPath(it, false)
            }.toList()
          RoutePath(root, children)
        }
      } catch (e: Exception) {
        throw IllegalArgumentException("RoutePath解析失败: $path", e)
      }
    }
  }
}

abstract class Action(val path: RoutePath, val methods: List<String>) {

  abstract operator fun invoke(request: AbstractHttpRequest): HttpResult

  override fun toString(): String {
    return "Route(path=$path, methods=$methods)"
  }

  fun acceptMethod(method: String): Boolean {
    if (methods.isEmpty()) {
      return true
    }
    val upperCaseMethod = method.toUpperCase()
    for (i in methods.indices) {
      if (methods[i] == upperCaseMethod) {
        return true
      }
    }
    return false
  }
}

data class ParPath(val name: String, val isVariable: Boolean)
