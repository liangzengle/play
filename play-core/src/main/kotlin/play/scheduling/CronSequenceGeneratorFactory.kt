package play.scheduling

internal class CronSequenceGeneratorFactory : CronExpression.Factory {
  override fun parse(cron: String): CronExpression {
    return CronSequenceGenerator(cron)
  }
}
