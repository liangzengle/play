package play.db.mongo

import org.bson.Document
import org.bson.conversions.Bson
import play.db.CommandSupport
import reactor.core.publisher.Flux

/**
 *
 * @author LiangZengle
 */
interface MongoDBCommandSupport : CommandSupport<Bson, Document> {

  override fun runCommand(cmd: Bson): Flux<Document>
}
