package play.db.mongo

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import play.entity.Entity
import play.util.reflect.ClassScanner

/**
 *
 * @author LiangZengle
 */
class MongoDBIndexBuilder(private val classScanner: ClassScanner, private val repository: MongoDBRepository) {

  private val subscriber = object : Subscriber<String?> {
    override fun onSubscribe(s: Subscription) {
    }

    override fun onNext(t: String?) {
    }

    override fun onError(t: Throwable) {
    }

    override fun onComplete() {
    }
  }

  fun buildAllIndexes() {
    val entityClasses = classScanner.getOrdinarySubclasses(Entity::class.java)
    for (entityClass in entityClasses) {
      buildIndex(entityClass)
    }
  }

  private fun buildIndex(clazz: Class<Entity<*>>) {
    
  }
}
