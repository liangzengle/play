package play.util.concurrent

fun go(taskName: String, task: () -> Unit) {
  Thread.ofVirtual().name(taskName).start(task)
}
