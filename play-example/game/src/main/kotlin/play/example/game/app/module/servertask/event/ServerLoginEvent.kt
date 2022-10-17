package play.example.game.app.module.servertask.event

import play.example.game.app.module.servertask.domain.ServerTaskTargetType

/**
 *
 *
 * @author LiangZengle
 */
data object ServerLoginEvent : ServerTaskEvent(ServerTaskTargetType.ServerLogin)
