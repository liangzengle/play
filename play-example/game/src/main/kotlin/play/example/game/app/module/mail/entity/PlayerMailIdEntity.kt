package play.example.game.app.module.mail.entity

import org.eclipse.collections.api.set.primitive.IntSet
import org.eclipse.collections.impl.factory.primitive.IntSets
import play.example.game.app.module.player.entity.AbstractPlayerEntity

class PlayerMailIdEntity(id: Long) : AbstractPlayerEntity(id) {
  private val mailIds = IntSets.mutable.empty()

  @Transient
  private var currentMailId = 0

  fun add(mailId: Int) = mailIds.add(mailId)

  fun remove(mailId: Int) = mailIds.remove(mailId)

  fun count() = mailIds.size()

  fun nextMailId(): Int {
    var nextId = currentMailId + 1
    if (nextId <= 0) {
      nextId = 1
    }
    while (mailIds.contains(nextId)) {
      nextId++
      if (nextId <= 0) {
        nextId = 1
      }
    }
    currentMailId = nextId
    return nextId
  }

  override fun initialize() {
    if (!mailIds.isEmpty) {
      currentMailId = mailIds.max()
    }
  }

  fun getMailIds(): IntSet {
    return mailIds
  }
}
