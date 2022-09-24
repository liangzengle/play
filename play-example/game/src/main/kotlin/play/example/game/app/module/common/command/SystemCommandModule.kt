package play.example.game.app.module.common.command

import play.example.game.container.command.Command
import play.example.game.container.command.CommandModule
import play.example.game.container.command.CommandResult
import play.util.time.Time
import java.time.Duration
import java.time.LocalDateTime

/**
 *
 *
 * @author LiangZengle
 */
@CommandModule("System", "X系统")
class SystemCommandModule {

  @Command(desc = "设置服务器时间，例: 2025-01-01T10:59:00、12h、3d1h5m10s")
  fun setTime(timeStr: String): CommandResult {
    val now = Time.currentDateTime()
    val result = Result.runCatching { LocalDateTime.parse(timeStr) }
      .map { Duration.between(now, LocalDateTime.parse(timeStr)) }
      .recoverCatching { Time.parseDuration(timeStr) }
    if (result.isFailure) {
      return CommandResult.err("设置时间失败, 参数错误: $timeStr")
    }
    val offset = result.getOrThrow()
    if (offset.isNegative) {
      return CommandResult.err("禁止回调时间, 当前时间: $now")
    }
    Time.setClockOffset(offset)
    return CommandResult.ok("服务器时间修改成功: ${Time.currentDateTime()}")
  }


}
