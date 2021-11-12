@file:JvmName("Cron")

package play.example.common.scheduling

object Cron {
  const val EveryDay = "0 0 0 * * *"
  const val EveryHour = "0 0 * * * *"
  const val EverySecond = "* * * * * *"
}
