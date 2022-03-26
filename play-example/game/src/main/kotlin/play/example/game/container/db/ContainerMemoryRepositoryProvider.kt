package play.example.game.container.db

import play.db.Repository
import play.db.memory.MemoryRepository

/**
 *
 * @author LiangZengle
 */
class ContainerMemoryRepositoryProvider : ContainerRepositoryProvider {

  private val repository = MemoryRepository()

  override fun get(): Repository {
    return repository
  }
}
