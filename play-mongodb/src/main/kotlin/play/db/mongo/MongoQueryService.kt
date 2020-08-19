package play.db.mongo

import org.bson.conversions.Bson
import play.db.ParameterizedQueryService

interface MongoQueryService : ParameterizedQueryService<Bson>
