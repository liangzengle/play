package play

object GlobalID {

  private const val CLASS_ID_MULTIPLIER = 1_000_000

  private const val MAX_CLASS_ID = 2000

  @JvmStatic
  fun getClassID(globalId: Int): Int = globalId / CLASS_ID_MULTIPLIER

  @JvmStatic
  fun getInstanceID(globalId: Int): Int = globalId % CLASS_ID_MULTIPLIER

  @JvmStatic
  fun toGlobalID(classId: Int, instanceId: Int): Int {
    require(classId in 1..MAX_CLASS_ID) { "class id out of range: [1, $MAX_CLASS_ID]" }
    require(instanceId in 0..CLASS_ID_MULTIPLIER) { "instance id out of range: [0, $CLASS_ID_MULTIPLIER)" }
    return classId * CLASS_ID_MULTIPLIER + instanceId
  }
}
