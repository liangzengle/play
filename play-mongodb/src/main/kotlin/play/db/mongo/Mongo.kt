@file:JvmName("Mongo")

package play.db.mongo

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.client.model.IndexOptions
import com.mongodb.connection.netty.NettyStreamFactoryFactory
import com.typesafe.config.Config
import de.undercouch.bson4jackson.BsonFactory
import de.undercouch.bson4jackson.BsonGenerator
import de.undercouch.bson4jackson.BsonParser
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import org.bson.codecs.configuration.CodecRegistries
import play.db.mongo.codec.EntityCodecProvider
import play.db.mongo.codec.MongoIntIdMixIn
import play.db.mongo.codec.MongoLongIdMixIn
import play.db.mongo.codec.MongoObjIdMixIn
import play.entity.Entity
import play.entity.IntIdEntity
import play.entity.LongIdEntity
import play.entity.ObjIdEntity
import play.util.json.Json
import java.util.concurrent.TimeUnit

object Mongo {
  const val ID = "_id"

  fun newClientSettings(conf: Config, eventLoopGroup: EventLoopGroup): MongoClientSettings {
    return newClientSettingsBuilder(conf, eventLoopGroup).build()
  }

  fun newClientSettingsBuilder(conf: Config, eventLoopGroup: EventLoopGroup): MongoClientSettings.Builder {
    val builder = MongoClientSettings.builder()
    if (conf.getBoolean("auth")) {
      val user = conf.getString("username")
      val pwd = conf.getString("password")
      val authSource = conf.getString("auth-source")
      builder.credential(MongoCredential.createCredential(user, authSource, pwd.toCharArray()))
    }
    val bsonFactory = BsonFactory().enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH)
      .enable(BsonGenerator.Feature.WRITE_BIGDECIMALS_AS_DECIMAL128)
    val objectMapper = Json.configure(ObjectMapper(bsonFactory)).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
      .addMixIn(LongIdEntity::class.java, MongoLongIdMixIn::class.java)
      .addMixIn(IntIdEntity::class.java, MongoIntIdMixIn::class.java)
      .addMixIn(ObjIdEntity::class.java, MongoObjIdMixIn::class.java)
    val entityCodecProvider = EntityCodecProvider(objectMapper)
    val codecRegistry = CodecRegistries.fromRegistries(
      CodecRegistries.fromProviders(entityCodecProvider), MongoClientSettings.getDefaultCodecRegistry()
    )
    val nThread = conf.getInt("client-threads")
    builder.applyToConnectionPoolSettings {
      it.maxSize(nThread).maxWaitTime(10, TimeUnit.SECONDS)
    }
    builder.streamFactoryFactory(newNettyStreamFactory(eventLoopGroup))
    builder.codecRegistry(codecRegistry)
    return builder
  }

  private fun newNettyStreamFactory(eventLoopGroup: EventLoopGroup): NettyStreamFactoryFactory {
    val builder = NettyStreamFactoryFactory.builder()
    if (eventLoopGroup is EpollEventLoopGroup) {
      builder.eventLoopGroup(eventLoopGroup).socketChannelClass(EpollSocketChannel::class.java)
    } else {
      builder.eventLoopGroup(eventLoopGroup).socketChannelClass(NioSocketChannel::class.java)
    }
    return builder.build()
  }

  fun ensureIndexes(repository: MongoDBRepository, entityClasses: List<Class<Entity<*>>>) {
    entityClasses.parallelStream().forEach { entityClass ->
      val indexes = entityClass.getAnnotationsByType(Index::class.java)
      for (index in indexes) {
        if (index.fields.isEmpty()) continue
        val fields = index.fields.copyOf()
        for (i in fields.indices) {
          val field = fields[i]
          if (field.startsWith("id.")) {
            fields[i] = field.replaceRange(0..2, "_id.")
          }
        }
        val opts = IndexOptions()
        opts.unique(index.unique)
        opts.sparse(index.sparse)
        val indexBson = index.type.parse(fields)
        val collection = repository.getCollection(entityClass)
        collection.createIndex(indexBson, opts)
      }
    }
  }

}
