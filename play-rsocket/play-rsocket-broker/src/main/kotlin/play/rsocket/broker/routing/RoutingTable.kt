package play.rsocket.broker.routing

import io.rsocket.RSocket
import play.rsocket.broker.util.IndexedMap
import play.rsocket.broker.util.IntIdByteIndexMap

/**
 *
 * @author LiangZengle
 */
class RoutingTable(private val table: IntIdByteIndexMap<RSocket>) : IndexedMap<Int, RSocket, Byte> by table
