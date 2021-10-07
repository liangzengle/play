package play.scheduling

import play.Log
import play.SystemProps
import play.util.ClassUtil
import play.util.reflect.Reflect
import java.time.LocalDateTime
import javax.annotation.Nonnull
import javax.annotation.Nullable

interface CronExpression {

  companion object {
    private val factory: Factory

    init {
      val qualifiedName =
        SystemProps.getOrDefault("cron.expression.factory", CronSequenceGeneratorFactory::class.qualifiedName)
      val factoryType = ClassUtil.loadClass<Factory>(qualifiedName)
      factory = Reflect.createInstance(factoryType)
      Log.debug { "Using $qualifiedName" }
    }

    @JvmStatic
    fun parse(cron: String): CronExpression = factory.parse(cron)
  }

  interface Factory {
    fun parse(cron: String): CronExpression
  }

  fun getExpression(): String

  @Nullable
  fun prevFireTime(from: LocalDateTime): LocalDateTime?

  @Nonnull
  fun nextFireTime(from: LocalDateTime): LocalDateTime
}

fun main() {
  val factoryType = ClassUtil.loadClass<CronExpression.Factory>("play.scheduling.CronSequenceGeneratorFactory")
  println(factoryType)
}
