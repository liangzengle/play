package play.example.game.container.db

import com.mongodb.reactivestreams.client.MongoClient
import com.typesafe.config.Config
import org.springframework.beans.factory.annotation.Autowired
import play.db.Repository
import play.db.TableNameResolver
import play.db.mongo.MongoDBRepository

/**
 *
 * @author LiangZengle
 */
class ContainerMongoDBRepositoryProvider @Autowired constructor(
  tableNameResolver: TableNameResolver,
  client: MongoClient,
  config: Config
) : ContainerRepositoryProvider {
  private val repository = MongoDBRepository(config.getString("play.container.db"), tableNameResolver, client)

  override fun get(): Repository {
    return repository
  }
}
