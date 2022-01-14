/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package play.scheduling

import org.slf4j.LoggerFactory
import play.util.exception.rethrow

/**
 * copied from spring framework
 *
 * Utility methods for decorating tasks with error handling.
 *
 *
 * **NOTE:** This class is intended for internal use by Spring's scheduler
 * implementations. It is only public so that it may be accessed from impl classes
 * within other packages. It is *not* intended for general use.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
internal object TaskUtils {
  /**
   * An ErrorHandler strategy that will log the Exception but perform
   * no further handling. This will suppress the error so that
   * subsequent executions of the task will not be prevented.
   */
  private val LOG_AND_SUPPRESS_ERROR_HANDLER: ErrorHandler = LoggingErrorHandler()

  /**
   * An ErrorHandler strategy that will log at error level and then
   * re-throw the Exception. Note: this will typically prevent subsequent
   * execution of a scheduled task.
   */
  private val LOG_AND_PROPAGATE_ERROR_HANDLER: ErrorHandler = PropagatingErrorHandler()

  /**
   * Decorate the task for error handling. If the provided [ErrorHandler]
   * is not `null`, it will be used. Otherwise, repeating tasks will have
   * errors suppressed by default whereas one-shot tasks will have errors
   * propagated by default since those errors may be expected through the
   * returned [Future]. In both cases, the errors will be logged.
   */
  fun decorateTaskWithErrorHandler(
    task: Runnable?, errorHandler: ErrorHandler?, isRepeatingTask: Boolean
  ): DelegatingErrorHandlingRunnable {
    if (task is DelegatingErrorHandlingRunnable) {
      return task
    }
    val eh = errorHandler ?: getDefaultErrorHandler(isRepeatingTask)
    return DelegatingErrorHandlingRunnable(task!!, eh)
  }

  /**
   * Return the default [ErrorHandler] implementation based on the boolean
   * value indicating whether the task will be repeating or not. For repeating tasks
   * it will suppress errors, but for one-time tasks it will propagate. In both
   * cases, the error will be logged.
   */
  fun getDefaultErrorHandler(isRepeatingTask: Boolean): ErrorHandler {
    return if (isRepeatingTask) LOG_AND_SUPPRESS_ERROR_HANDLER else LOG_AND_PROPAGATE_ERROR_HANDLER
  }

  /**
   * An [ErrorHandler] implementation that logs the Throwable at error
   * level. It does not perform any additional error handling. This can be
   * useful when suppression of errors is the intended behavior.
   */
  private open class LoggingErrorHandler : ErrorHandler {
    private val logger = LoggerFactory.getLogger(LoggingErrorHandler::class.java)
    override fun handleError(t: Throwable) {
      logger.error("Unexpected error occurred in scheduled task", t)
    }
  }

  /**
   * An [ErrorHandler] implementation that logs the Throwable at error
   * level and then propagates it.
   */
  private class PropagatingErrorHandler : LoggingErrorHandler() {
    override fun handleError(t: Throwable) {
      super.handleError(t)
      t.rethrow()
    }
  }
}
