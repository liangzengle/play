package play

object GlobalID {

  private const val CLASS_ID_MULTIPLIER = 100000

  const val MAX_CLASS_ID = Int.MAX_VALUE / CLASS_ID_MULTIPLIER

  @JvmStatic
  fun getClassID(globalId: Int): Int = globalId / CLASS_ID_MULTIPLIER

  @JvmStatic
  fun getInstanceID(globalId: Int): Int = globalId - (getClassID(globalId) * CLASS_ID_MULTIPLIER)

  @JvmStatic
  fun toGlobalID(classId: Int, instanceId: Int): Int {
    if (classId < 0 || classId > MAX_CLASS_ID || instanceId < 0 || instanceId > CLASS_ID_MULTIPLIER) {
      throw IllegalArgumentException()
    }
    return Math.toIntExact(classId.toLong() * CLASS_ID_MULTIPLIER + instanceId)
  }
}
