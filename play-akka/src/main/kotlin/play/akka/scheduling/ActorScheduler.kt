package play.akka.scheduling

import akka.actor.typed.ActorRef
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive
import mu.KLogging
import play.akka.AbstractTypedActor
import play.scheduling.*
import play.util.unsafeCast
import java.time.Duration
import java.time.LocalDateTime

class ActorScheduler(ctx: ActorContext<Command>, private val scheduler: Scheduler) :
  AbstractTypedActor<ActorScheduler.Command>(ctx) {
  companion object : KLogging() {
    @JvmStatic
    private fun triggerAction(triggerEvent: Any, receiver: ActorRef<Any>, me: ActorRef<Command>): () -> Unit = {
      receiver.tell(triggerEvent)
      me.tell(LogScheduleTriggered(triggerEvent, receiver))
    }
  }

  private val scheduleMap = hashMapOf<ActorRef<*>, MutableMap<Any, Cancellable>>()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::schedule)
      .accept(::scheduleAt)
      .accept(::scheduleWithTimeout)
      .accept(::scheduleCron)
      .accept(::cancel)
      .accept(::cancelAll)
      .accept(::logTriggered)
      .acceptSignal(::onCommanderTerminated)
      .build()
  }

  private fun logTriggered(msg: LogScheduleTriggered) {
    logger.info("schedule triggered: {} {}", msg.receiver, msg.triggerEvent)
  }

  private fun scheduleCron(cmd: ScheduleCron<Any>) {
    val cron = cmd.cron
    val trigger = try {
      CronTrigger(CronExpression.parse(cron))
    } catch (e: Exception) {
      logger.error(e) {"failed to parse cron: $cron"}
      return
    }
    schedule(trigger, cmd.triggerEvent, cmd.commander.unsafeCast())
  }

  private fun schedule(cmd: Schedule<Any>) {
    schedule(cmd.trigger, cmd.triggerEvent, cmd.commander.unsafeCast())
  }

  private fun scheduleAt(cmd: ScheduleAt<Any>) {
    scheduleAt(cmd.triggerTime, cmd.triggerEvent, cmd.commander.unsafeCast())
  }

  private fun scheduleWithTimeout(cmd: ScheduleWithTimeout<Any>) {
    scheduleWithTimeout(cmd.timeout, cmd.triggerEvent, cmd.commander.unsafeCast())
  }

  private fun schedule(trigger: Trigger, triggerEvent: Any, receiver: ActorRef<Any>) {
    val cancellable = scheduler.schedule(trigger, ec, triggerAction(triggerEvent, receiver, self))
    schedule(cancellable, triggerEvent, receiver)
  }

  private fun scheduleAt(triggerTime: LocalDateTime, triggerEvent: Any, receiver: ActorRef<Any>) {
    val cancellable = scheduler.scheduleAt(triggerTime, ec, triggerAction(triggerEvent, receiver, self))
    schedule(cancellable, triggerEvent, receiver)
  }

  private fun scheduleWithTimeout(timeout: Duration, triggerEvent: Any, receiver: ActorRef<Any>) {
    val cancellable = scheduler.schedule(timeout, ec, triggerAction(triggerEvent, receiver, self))
    schedule(cancellable, triggerEvent, receiver)
  }

  private fun schedule(cancellable: Cancellable, triggerEvent: Any, receiver: ActorRef<Any>) {
    logger.info("schedule added: {} {}", receiver, triggerEvent)
    var map = scheduleMap[receiver]
    if (map == null) {
      map = HashMap(4)
      scheduleMap[receiver] = map
      context.watch(receiver)
    }
    val prev = map.put(triggerEvent, cancellable)
    if (prev != null) {
      prev.cancel()
      logger.info("schedule replaced: {} {}", receiver, triggerEvent)
    }
  }

  private fun cancel(cmd: Cancel<Any>) {
    scheduleMap[cmd.commander]?.remove(cmd.triggerEvent)?.cancel()
    logger.info("schedule cancelled: {}", cmd)
  }

  private fun cancelAll(cmd: CancelAll<Any>) {
    val commander = cmd.commander
    val cancelEventType = cmd.triggerEventType
    // 移除所有定时任务
    if (cancelEventType == null) {
      scheduleMap.remove(commander)?.values?.forEach { it.cancel() }
      context.unwatch(commander)
      logger.info("schedule cancel all: {}", commander)
      return
    }
    // 移除特定类型的定时任务
    val schedules = scheduleMap[commander] ?: return
    val it = schedules.entries.iterator()
    while (it.hasNext()) {
      val (event, cancellable) = it.next()
      if (cancelEventType.isAssignableFrom(event.javaClass)) {
        it.remove()
        cancellable.cancel()
        logger.info("schedule cancel by type: {} {}", commander, event)
      }
    }
    if (schedules.isEmpty()) {
      scheduleMap.remove(commander)
      context.unwatch(commander)
    }
  }

  private fun onCommanderTerminated(msg: Terminated) {
    val commander = msg.ref
    scheduleMap.remove(commander)?.values?.forEach { it.cancel() }
    logger.info("schedule commander terminated, cancelling all its schedules: {}", commander)
  }

  interface Command

  private class LogScheduleTriggered(val triggerEvent: Any, val receiver: ActorRef<Any>) : Command

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
   * @property triggerTime 触发时间
   * @property triggerEvent 触发事件
   * @property commander 事件接收者
   */
  data class ScheduleAt<T>(val triggerTime: LocalDateTime, val triggerEvent: T, val commander: ActorRef<out T>) :
    Command

  /**
   * 添加定时任务
   *
   * @property timeout 触发延迟
   * @property triggerEvent 触发事件
   * @property commander 事件接收者
   */
  data class ScheduleWithTimeout<T>(val timeout: Duration, val triggerEvent: T, val commander: ActorRef<out T>) :
    Command

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
   * 取消一类定时任务
   *
   * @param commander 事件接收者
   * @param triggerEventType 事件类型
   */
  data class CancelAll<T> @JvmOverloads constructor(
    val commander: ActorRef<out T>,
    val triggerEventType: Class<out T>? = null
  ) : Command
}
