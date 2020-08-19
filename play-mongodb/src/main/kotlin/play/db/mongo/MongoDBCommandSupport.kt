package play.db.mongo

import org.bson.Document
import org.bson.conversions.Bson
import play.db.CommandSupport
import play.util.concurrent.Future

/**
 *
 * @author LiangZengle
 */
interface MongoDBCommandSupport : CommandSupport<Bson, Document> {

  override fun runCommand(cmd: Bson): Future<Document>
}
