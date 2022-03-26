package play.example.game.container.db

import play.db.Repository

/**
 *
 * @author LiangZengle
 */
interface ContainerRepositoryProvider {
  fun get(): Repository
}
