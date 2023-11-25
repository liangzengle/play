package play

import com.google.common.collect.ComparisonChain
import play.util.splitToInts

data class PlayVersion(val major: Int, val minor: Int, val patch: Int, val build: Int) : Comparable<PlayVersion> {

  companion object {
    @JvmStatic
    fun parse(versionString: String): PlayVersion {
      val ints = versionString.splitToInts('.')
      return PlayVersion(ints.nextInt(), ints.nextInt(), ints.nextInt(), ints.nextInt())
    }
  }

  override fun compareTo(other: PlayVersion): Int {
    return ComparisonChain.start()
      .compare(other.major, major)
      .compare(other.minor, minor)
      .compare(other.patch, patch)
      .compare(build, build)
      .result()
  }

  override fun toString(): String {
    return "$major.$minor.$patch.$build"
  }
}
