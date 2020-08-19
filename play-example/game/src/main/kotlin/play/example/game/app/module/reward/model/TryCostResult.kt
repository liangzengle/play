package play.example.game.app.module.reward.model

abstract class TryCostResultSetLike {
  abstract fun appendTo(results: MutableList<TryCostResult>)
  abstract fun asList(): List<TryCostResult>
}

data class TryCostResult(val cost: Reward, val changeCount: Long = 0) : TryCostResultSetLike() {
  val costCount = cost.num + changeCount

  override fun appendTo(results: MutableList<TryCostResult>) {
    results.add(this)
  }

  override fun asList(): List<TryCostResult> = listOf(this)
}

data class TryCostResultSet(val results: List<TryCostResult>, val logSource: Int) : TryCostResultSetLike() {
  override fun appendTo(results: MutableList<TryCostResult>) {
    results.addAll(this.results)
  }

  override fun asList(): List<TryCostResult> = results

  fun isEmpty() = results.isEmpty()
}
