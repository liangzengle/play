package play.util

/**
 *
 * @author LiangZengle
 */
object Sorting {
  /**
   * 深度优先拓扑排序
   *
   * @param nodes 排序的元素
   * @param preNodes 获取元素前置元素
   * @return List<E>
   */
  @JvmStatic
  fun <E> topologicalSort(nodes: Collection<E>, preNodes: (E) -> Iterable<E>): List<E> {
    val result = ArrayList<E>(nodes.size)
    val unvisited = (nodes.asSequence() + nodes.asSequence().flatMap(preNodes)).toMutableSet()
    val visited = linkedSetOf<E>()

    fun depthFirstSearch(e: E) {
      if (visited.contains(e)) {
        throw IllegalArgumentException("Cycle detected: ${visited.joinToString("->")}->$e")
      }
      if (unvisited.contains(e)) {
        visited.add(e)
        preNodes(e).forEach(::depthFirstSearch)
        unvisited.remove(e)
        visited.remove(e)
        result.add(e)
      }
    }
    while (unvisited.isNotEmpty()) {
      depthFirstSearch(unvisited.first())
    }
    return result
  }
}
