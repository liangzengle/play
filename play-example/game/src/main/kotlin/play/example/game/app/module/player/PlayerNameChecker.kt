package play.example.game.app.module.player

import play.util.concurrent.PlayFuture

/**
 *
 * @author LiangZengle
 */
interface PlayerNameChecker {

  fun check(name: String): PlayFuture<Boolean>
}
