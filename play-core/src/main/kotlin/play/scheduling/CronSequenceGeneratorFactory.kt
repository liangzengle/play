package play.scheduling

internal object CronSequenceGeneratorFactory : CronExpression.Factory {
  override fun parse(cron: String): CronExpression {
    return CronSequenceGenerator(cron)
  }
}
