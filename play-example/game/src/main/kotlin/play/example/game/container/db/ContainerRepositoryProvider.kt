package play.example.game.container.db

import com.mongodb.reactivestreams.client.MongoClient
import com.typesafe.config.Config
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.db.Repository
import play.db.TableNameResolver
import play.db.mongo.MongoDBRepository

/**
 *
 * @author LiangZengle
 */
@Component
class ContainerRepositoryProvider @Autowired constructor(
  tableNameResolver: TableNameResolver,
  client: MongoClient,
  config: Config
) {
  private val repository = MongoDBRepository(config.getString("play.container.db"), tableNameResolver, client)

  fun get(): Repository {
    return repository
  }
}
