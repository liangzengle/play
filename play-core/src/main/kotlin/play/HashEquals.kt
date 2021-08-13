package play

interface HashEquals {

  override fun hashCode(): Int

  override fun equals(other: Any?): Boolean
}
