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
      if (action.path.matches(path)) {
        return Optional.of(action)
      }
    }
    return Optional.empty()
  }

  @Synchronized
  fun register(actions: Collection<Action>) {
    val plainActions = this.plainActions.toMutableMap()
    val variableActions = this.variableActions.toMutableList()
    for (action in actions) {
      if (action.path.isPlain()) {
        val prev = plainActions.putIfAbsent(action.path.root, action)
        require(prev == null) { "Duplicated Action: $action" }
      } else {
        variableActions.add(action)
      }
    }
    variableActions.sortBy { it.path.root }
    this.plainActions = plainActions
    this.variableActions = variableActions
  }
}
