package play.scheduling

import play.Log
import play.StaticConfigurator
import java.time.Instant
import javax.annotation.Nonnull
import javax.annotation.Nullable

interface CronExpression {

  companion object {
    private val factory: Factory =
      StaticConfigurator.getOrDefault(Factory::class.java) { CronSequenceGeneratorFactory }

    init {
      Log.debug { "CronExpression.Factory: ${factory.javaClass.simpleName}" }
    }

    @JvmStatic
    fun parse(cron: String): CronExpression = factory.parse(cron)
  }

  interface Factory {
    fun parse(cron: String): CronExpression
  }

  fun getExpression(): String

  @Nullable
  fun prevFireTime(from: Instant): Instant?

  @Nonnull
  fun nextFireTime(from: Instant): Instant
}
