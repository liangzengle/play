package play.rsocket.metadata

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.rsocket.metadata.WellKnownMimeType

/**
 * metadata for routing
 *
 * @author LiangZengle
 */
class RoutingMetadata private constructor(
  @JvmField val nodeId: Int,
  @JvmField val nodeIds: IntArray,
  @JvmField val role: Byte,
  @JvmField val roles: ByteArray,
  @JvmField val routingType: RoutingType
) : MetadataEntry {

  companion object {

    private val EmptyIntArray = IntArray(0)
    private val EmptyByteArray = ByteArray(0)

    @JvmStatic
    val DefaultInstance = RoutingMetadata(0, EmptyIntArray, 0, EmptyByteArray, RoutingType.UnicastToNode)

    /**
     * 指定的一个节点
     *
     * @param nodeId 节点id
     * @return RoutingMetadata
     */
    @JvmStatic
    fun oneNode(nodeId: Int): RoutingMetadata {
      return RoutingMetadata(nodeId, EmptyIntArray, 0, EmptyByteArray, RoutingType.UnicastToNode)
    }

    /**
     * 指定的所有节点
     *
     * @param nodeIds IntArray
     * @return RoutingMetadata
     */
    @JvmStatic
    fun allNodes(nodeIds: IntArray): RoutingMetadata {
      return RoutingMetadata(0, nodeIds, 0, EmptyByteArray, RoutingType.MulticastToNodes)
    }

    /**
     * 指定的所有节点
     *
     * @param nodeIds Collection<Int>
     * @return RoutingMetadata
     */
    @JvmStatic
    fun allNodes(nodeIds: Collection<Int>): RoutingMetadata {
      return RoutingMetadata(0, nodeIds.toIntArray(), 0, EmptyByteArray, RoutingType.MulticastToNodes)
    }

    /**
     * 角色为[role]的其中一个节点
     *
     * @param role 角色
     * @return RoutingMetadata
     */
    @JvmStatic
    fun oneNodeOfRole(role: Byte): RoutingMetadata {
      return RoutingMetadata(0, EmptyIntArray, role, EmptyByteArray, RoutingType.UnicastToRole)
    }

    /**
     * 角色为[role]的所有节点
     *
     * @param role 角色
     * @return RoutingMetadata
     */
    @JvmStatic
    fun allNodesOfRole(role: Byte): RoutingMetadata {
      return RoutingMetadata(0, EmptyIntArray, role, EmptyByteArray, RoutingType.MulticastToRole)
    }

    /**
     * 角色为[roles]之一的所有节点
     *
     * @param roles 角色列表
     * @return RoutingMetadata
     */
    @JvmStatic
    fun allNodesOfRoles(roles: ByteArray): RoutingMetadata {
      return RoutingMetadata(0, EmptyIntArray, 0, roles, RoutingType.MulticastToRole)
    }

    /**
     * 角色为[roles]之一的所有节点
     *
     * @param roles 角色列表
     * @return RoutingMetadata
     */
    @JvmStatic
    fun allNodesOfRoles(roles: Collection<Byte>): RoutingMetadata {
      return RoutingMetadata(0, EmptyIntArray, 0, roles.toByteArray(), RoutingType.MulticastToRole)
    }
  }

  override fun getMimeType(): String {
    return WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.string
  }

  override fun parseFrom(data: ByteBuf): RoutingMetadata {
    val routingTypeId = data.getByte(0)
    var offset = 1
    var nodeId = 0
    var nodeIds = EmptyIntArray
    var role: Byte = 0
    var roles = EmptyByteArray
    val routingType: RoutingType
    when (routingTypeId) {
      RoutingType.UnicastToNode.id -> {
        routingType = RoutingType.UnicastToNode
        nodeId = data.getInt(offset)
        offset += 4
      }

      RoutingType.UnicastToRole.id -> {
        routingType = RoutingType.UnicastToRole
        role = data.getByte(offset)
      }

      RoutingType.MulticastToNodes.id -> {
        routingType = RoutingType.MulticastToNodes
        val size = data.getShort(offset)
        offset += 2
        val nodeIdArray = IntArray(size.toInt())
        var i = 0
        while (i < size) {
          nodeIdArray[i] = data.getInt(offset)
          i++
          offset += 4
        }
        nodeIds = nodeIdArray
      }

      RoutingType.MulticastToRole.id -> {
        routingType = RoutingType.MulticastToRole
        role = data.getByte(offset)
      }

      RoutingType.MulticastToRoles.id -> {
        routingType = RoutingType.MulticastToRoles
        val size = data.getByte(offset).toInt()
        offset++
        val roleArray = ByteArray(size)
        data.getBytes(offset, roleArray)
        roles = roleArray
      }

      else -> throw IllegalStateException("Unknown routing type: $routingTypeId")
    }
    return RoutingMetadata(nodeId, nodeIds, role, roles, routingType)
  }

  override fun getContent(): ByteBuf {
    val buffer = ByteBufAllocator.DEFAULT.buffer(computeSize())
    buffer.writeByte(routingType.id.toInt())
    when (routingType) {
      RoutingType.UnicastToNode -> {
        buffer.writeInt(nodeId)
      }

      RoutingType.UnicastToRole -> {
        buffer.writeByte(role.toInt())
      }

      RoutingType.MulticastToNodes -> {
        buffer.writeShort(nodeIds.size)
        nodeIds.forEach(buffer::writeInt)
      }

      RoutingType.MulticastToRole -> {
        buffer.writeByte(role.toInt())
      }

      RoutingType.MulticastToRoles -> {
        buffer.writeByte(roles.size)
        buffer.writeBytes(roles)
      }
    }
    return buffer
  }

  private fun computeSize(): Int {
    // routingType
    var size = 1
    size += when (routingType) {
      // nodeId
      RoutingType.UnicastToNode -> 4
      // role
      RoutingType.UnicastToRole -> 4
      // nodeIds.size + nodeIds
      RoutingType.MulticastToNodes -> (2 + nodeIds.size * 4)
      // role
      RoutingType.MulticastToRole -> 1
      // roles
      RoutingType.MulticastToRoles -> 1 + roles.size
    }
    return size
  }

  override fun toString(): String {
    val b = StringBuilder()
    when (routingType) {
      RoutingType.UnicastToNode -> b.append("nodeId=").append(nodeId)
      RoutingType.UnicastToRole, RoutingType.MulticastToRole -> b.append("role=").append(role)
        .append(", broadcastType=").append(routingType.broadcastType)

      RoutingType.MulticastToNodes -> b.append("nodeIds=").append(nodeIds.contentToString())
      RoutingType.MulticastToRoles -> b.append("roles=").append(roles.contentToString())
    }
    return b.toString()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RoutingMetadata) return false
    return this.nodeId == other.nodeId
      && this.nodeIds.contentEquals(other.nodeIds)
      && this.role == other.role
      && this.roles.contentEquals(other.roles)
  }

  override fun hashCode(): Int {
    var result = 1
    if (nodeId != 0) {
      result += result * 31 + nodeId.hashCode()
    }
    if (nodeIds.isNotEmpty()) {
      result += result * 31 + nodeIds.hashCode()
    }
    if (role != 0.toByte()) {
      result += result * 31 + role.hashCode()
    }
    if (roles.isNotEmpty()) {
      result += result * 31 + roles.hashCode()
    }
    result += result * 31 + routingType.id.hashCode()
    return result
  }
}
