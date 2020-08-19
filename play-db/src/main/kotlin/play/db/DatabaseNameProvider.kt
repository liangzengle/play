package play.db

/**
 *
 * @author LiangZengle
 */
fun interface DatabaseNameProvider {

  fun get(): String
}
