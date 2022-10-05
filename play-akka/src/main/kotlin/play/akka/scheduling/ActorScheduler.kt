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
    private const val ONCE: Byte = 1
    private const val REPEATED: Byte = 2
    private const val TRIGGER: Byte = 3
  }

  private val scheduleMap = hashMapOf<ActorRef<*>, MutableMap<Any, Cancellable>>()

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::schedule)
      .accept(::scheduleAt)
      .accept(::scheduleWithTimeout)
      .accept(::scheduleWithTimeout)
      .accept(::scheduleCron)
      .accept(::cancel)
      .accept(::cancelAll)
      .accept(::onTriggered)
      .acceptSignal(::onCommanderTerminated)
      .build()
  }

  private fun onTriggered(msg: ScheduleTriggered) {
    val receiver = msg.receiver
    val triggerEvent = msg.triggerEvent
    val handle = scheduleMap[receiver]?.get(triggerEvent)
    if (handle == null) {
      logger.info("discard triggered schedule event, it's most likely been cancelled: {} {}", receiver, triggerEvent)
      return
    }
    logger.debug("schedule triggered: {} {}", receiver, triggerEvent)
    receiver.tell(triggerEvent)
    when (msg.type) {
      ONCE -> removeSchedule(receiver, triggerEvent)
      TRIGGER -> {
        if (handle.isCancelled()) {
          removeSchedule(receiver, triggerEvent)
        }
      }
    }
  }

  private fun scheduleCron(cmd: ScheduleCron<Any>) {
    val cron = cmd.cron
    val trigger = try {
      CronTrigger(CronExpression.parse(cron))
    } catch (e: Exception) {
      logger.error(e) { "failed to parse cron: $cron" }
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

  private fun scheduleWithFixedDelay(cmd: ScheduleWithFixedDelay<Any>) {
    scheduleWithFixedDelay(cmd.initialDelay, cmd.delay, cmd.triggerEvent, cmd.commander.unsafeUpcast())
  }

  private fun schedule(trigger: Trigger, triggerEvent: Any, receiver: ActorRef<Any>) {
    val cancellable = scheduler.schedule(trigger, ec) { self.tell(ScheduleTriggered(triggerEvent, receiver, TRIGGER)) }
    addSchedule(cancellable, triggerEvent, receiver)
  }

  private fun scheduleAt(triggerTime: LocalDateTime, triggerEvent: Any, receiver: ActorRef<Any>) {
    val cancellable =
      scheduler.scheduleAt(triggerTime, ec) { self.tell(ScheduleTriggered(triggerEvent, receiver, ONCE)) }
    addSchedule(cancellable, triggerEvent, receiver)
  }

  private fun scheduleWithTimeout(timeout: Duration, triggerEvent: Any, receiver: ActorRef<Any>) {
    val cancellable = scheduler.schedule(timeout, ec) { self.tell(ScheduleTriggered(triggerEvent, receiver, ONCE)) }
    addSchedule(cancellable, triggerEvent, receiver)
  }

  private fun scheduleWithFixedDelay(
    initialDelay: Duration,
    delay: Duration,
    triggerEvent: Any,
    receiver: ActorRef<Any>
  ) {
    val cancellable =
      scheduler.scheduleWithFixedDelay(initialDelay, delay, ec) {
        self.tell(ScheduleTriggered(triggerEvent, receiver, REPEATED))
      }
    addSchedule(cancellable, triggerEvent, receiver)
  }

  private fun addSchedule(cancellable: Cancellable, triggerEvent: Any, receiver: ActorRef<Any>) {
    logger.debug("schedule added: {} {}", receiver, triggerEvent)
    var map = scheduleMap[receiver]
    if (map == null) {
      map = HashMap(4)
      scheduleMap[receiver] = map
      context.watch(receiver)
    }
    val prev = map.put(triggerEvent, cancellable)
    if (prev != null) {
      prev.cancel()
      logger.debug("schedule replaced: {} {}", receiver, triggerEvent)
    }
  }

  private fun removeSchedule(receiver: ActorRef<Any>, triggerEvent: Any) {
    val map = scheduleMap[receiver] ?: return
    val remove = map.remove(triggerEvent)
    if (remove != null) {
      logger.debug("schedule removed: {} {}", receiver, triggerEvent)
      if (map.isEmpty()) {
        scheduleMap.remove(receiver)
        context.unwatch(receiver)
      }
    }
  }

  private fun cancel(cmd: Cancel<Any>) {
    scheduleMap[cmd.commander]?.remove(cmd.triggerEvent)?.cancel()
    logger.debug("schedule cancelled: {}", cmd)
  }

  private fun cancelAll(cmd: CancelAll<Any>) {
    val commander = cmd.commander
    val cancelEventType = cmd.triggerEventType
    // 移除所有定时任务
    if (cancelEventType == null) {
      val map = scheduleMap.remove(commander)
      if (map != null) {
        map.values.forEach { it.cancel() }
        context.unwatch(commander)
        logger.debug("schedule cancel all: {}", commander)
      }
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
        logger.debug("schedule cancel by type: {} {}", commander, event)
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
    logger.debug("schedule commander terminated, cancelling all its schedules: {}", commander)
  }

  interface Command

  private class ScheduleTriggered(val triggerEvent: Any, val receiver: ActorRef<Any>, val type: Byte) : Command

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
   * @property initialDelay 首次触发延迟
   * @property delay 后续触发延迟
   * @property triggerEvent 触发事件
   * @property commander 事件接收者
   */
  data class ScheduleWithFixedDelay<T>(
    val initialDelay: Duration,
    val delay: Duration,
    val triggerEvent: T,
    val commander: ActorRef<out T>
  ) : Command {
    constructor(delay: Duration, triggerEvent: T, commander: ActorRef<out T>) : this(
      delay,
      delay,
      triggerEvent,
      commander
    )
  }

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
