/*
 * Copyright 2002-2012 the original author or authors.
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

import java.lang.reflect.UndeclaredThrowableException
import javax.annotation.Nonnull

/**
 * Runnable wrapper that catches any exception or error thrown from its
 * delegate Runnable and allows an [ErrorHandler] to handle it.
 *
 * @param delegate the Runnable implementation to delegate to
 * @param errorHandler the ErrorHandler for handling any exceptions
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 3.0
 */
internal open class DelegatingErrorHandlingRunnable(
  @param:Nonnull private val delegate: Runnable,
  @param:Nonnull private val errorHandler: ErrorHandler
) : Runnable {
  override fun run() {
    try {
      delegate.run()
    } catch (ex: UndeclaredThrowableException) {
      errorHandler.handleError(ex.undeclaredThrowable)
    } catch (ex: Throwable) {
      errorHandler.handleError(ex)
    }
  }

  override fun toString(): String {
    return "DelegatingErrorHandlingRunnable for $delegate"
  }
}
