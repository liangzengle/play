package play.example.common.akka.scheduling

import akka.actor.typed.ActorRef
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive
import mu.KLogging
import play.akka.AbstractTypedActor
import play.scheduling.*
import play.util.unsafeCast

class ActorScheduler(ctx: ActorContext<Command>, private val scheduler: Scheduler) :
  AbstractTypedActor<ActorScheduler.Command>(ctx) {
  companion object : KLogging()

   private val scheduleMap = hashMapOf<ActorRef<*>, MutableMap<Any, Cancellable>>()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::schedule)
      .accept(::scheduleCron)
      .accept(::cancel)
      .accept(::cancelAll)
      .acceptSignal(::onCommanderTerminated)
      .build()
  }

  private fun scheduleCron(cmd: ScheduleCron<Any>) {
    val trigger = CronTrigger(CronExpression.parse(cmd.cron))
    schedule(trigger, cmd.triggerEvent, cmd.commander.unsafeCast())
  }

  private fun schedule(cmd: Schedule<Any>) {
    schedule(cmd.trigger, cmd.triggerEvent, cmd.commander.unsafeCast())
  }

  private fun schedule(trigger: Trigger, triggerEvent: Any, receiver: ActorRef<Any>) {
    val cancellable = scheduler.schedule(trigger, context.executionContext) {
      receiver.tell(triggerEvent)
      logger.info { "cron schedule triggered: $receiver $triggerEvent" }
    }

    logger.info { "cron schedule added: $triggerEvent" }

    val watched = scheduleMap.containsKey(receiver)
    if (!watched) {
      context.watch(receiver)
    }
    val map = scheduleMap.computeIfAbsent(receiver) { hashMapOf() }
    val prev = map.put(triggerEvent, cancellable)
    if (prev != null) {
      prev.cancel()
      logger.info { "cron schedule replaced: $triggerEvent" }
    }
  }

  private fun cancel(cmd: Cancel<Any>) {
    scheduleMap[cmd.commander]?.remove(cmd.triggerEvent)?.cancel()
    logger.info { "cron schedule cancelled: $cmd" }
  }

  private fun cancelAll(cmd: CancelAll<Any>) {
    val commander = cmd.commander
    val cancelEventType = cmd.triggerEventType
    if (cancelEventType == null) {
      scheduleMap.remove(commander)?.values?.forEach { it.cancel() }
      context.unwatch(commander)
      logger.info { "cron schedule cancel all: $commander" }
    } else {
      val it = scheduleMap[commander]?.entries?.iterator()
      if (it != null) {
        while (it.hasNext()) {
          val (event, cancellable) = it.next()
          if (cancelEventType.isAssignableFrom(event.javaClass)) {
            it.remove()
            cancellable.cancel()
            logger.info { "cron schedule cancel certain type schedule: $event $commander" }
          }
        }
      }
    }
  }

  private fun onCommanderTerminated(msg: Terminated) {
    val commander = msg.ref
    scheduleMap.remove(commander)?.values?.forEach { it.cancel() }
    logger.info { "cron schedule commander terminated, cancel all its schedules: $commander" }
  }

  interface Command

  /**
   * 添加定时任务
   *
   * @param trigger 触发器
   * @param triggerEvent 触发事件
   * @param commander 事件接收者
   */
  data class Schedule<T>(val trigger: Trigger, val triggerEvent: T, val commander: ActorRef<out T>) : Command

  /**
   * 添加定时任务
   *
   * @param cron cron表达式
   * @param triggerEvent 触发事件
   * @param commander 事件接收者
   */
  data class ScheduleCron<T>(val cron: String, val triggerEvent: T, val commander: ActorRef<out T>) : Command

  /**
   * 取消某个定时任务
   *
   * @param triggerEvent 触发事件
   * @param commander 事件接收者
   */
  data class Cancel<T>(val triggerEvent: T, val commander: ActorRef<out T>) : Command

  /**
   * 取消定时任务
   *
   * @param commander 事件接收者
   * @param triggerEventType 事件类型
   */
  data class CancelAll<T>(val commander: ActorRef<out T>, val triggerEventType: Class<out T>?) : Command {
    constructor(commander: ActorRef<out T>) : this(commander, null)
  }
}
