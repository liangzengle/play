package play.net.http

import java.util.*

abstract class HttpActionManager {

  private var plainActions = emptyMap<String, Action>()

  private var variableActions = emptyList<Action>()

  fun findAction(path: String): Optional<Action> {
    val plainAction = plainActions[path]
    if (plainAction != null) {
      return Optional.of(plainAction)
    }
    for (i in variableActions.indices) {
      val action = variableActions[i]
      if (action.path.root > path) {
        break
      }
      if (action.path.matches(path)) {
        return Optional.of(action)
      }
    }
    return Optional.empty()
  }

  @Synchronized
  fun register(actions: Collection<Action>) {
    var plainActions: MutableMap<String, Action>? = null
    var variableActions: MutableList<Action>? = null
    for (action in actions) {
      if (action.path.isPlain()) {
        if (plainActions == null) {
          plainActions = this.plainActions.toMutableMap()
        }
        val prev = plainActions.putIfAbsent(action.path.root, action)
        require(prev == null) { "Duplicated Action: $action" }
      } else {
        if (variableActions == null) {
          variableActions = this.variableActions.toMutableList()
        }
        variableActions.add(action)
      }
    }
    if (plainActions != null) {
      this.plainActions = plainActions
    }
    if (variableActions != null) {
      variableActions.sortBy { it.path.root }
      this.variableActions = variableActions
    }
  }
}
