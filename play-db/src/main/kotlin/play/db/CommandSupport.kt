package play.db

import reactor.core.publisher.Flux

/**
 *
 * @author LiangZengle
 */
interface CommandSupport<IN, OUT> {

  fun runCommand(cmd: IN): Flux<out OUT>
}
