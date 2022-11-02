package play.net.http

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import java.util.concurrent.atomic.AtomicReference

abstract class HttpActionManager {

  private val actionQuery = AtomicReference(ActionQuery(emptyMap(), emptyList()))

  open fun findAction(path: String, uri: String): Action? {
    return findAction(path)
  }

  fun findAction(path: String): Action? {
    return actionQuery.get().find(path)
  }

  fun register(actions: Collection<Action>) {
    this.actionQuery.updateAndGet { it.plus(actions) }
  }

  private class ActionQuery(private val plainActions: Map<String, Action>, private val variableActions: List<Action>) {
    fun plus(actions: Collection<Action>): ActionQuery {
      var plainActionMap: MutableMap<String, Action>? = null
      var variableActionList: MutableList<Action>? = null
      for (action in actions) {
        if (action.path.isPlain()) {
          if (plainActionMap == null) {
            plainActionMap = this.plainActions.toMutableMap()
          }
          val prev = plainActionMap.putIfAbsent(action.path.root, action)
          require(prev == null) { "Duplicated Action: $action" }
        } else {
          if (variableActionList == null) {
            variableActionList = this.variableActions.toMutableList()
          }
          variableActionList.add(action)
        }
      }
      val newPlainActionMap = if (plainActionMap != null) ImmutableMap.copyOf(plainActionMap) else this.plainActions
      val newVariableActionList =
        if (variableActionList != null) {
          variableActionList.sortBy { it.path.root }
          ImmutableList.copyOf(variableActionList)
        } else this.variableActions
      return ActionQuery(newPlainActionMap, newVariableActionList)
    }

    fun find(path: String): Action? {
      val plainAction = plainActions[path]
      if (plainAction != null) {
        return plainAction
      }
      for (action in variableActions) {
        if (action.path.root > path) {
          break
        }
        if (action.path.matches(path)) {
          return action
        }
      }
      return null
    }
  }
}
