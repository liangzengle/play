package play.util.logging

import org.slf4j.MDC

inline fun withMDC(key: String, value: String, action: () -> Unit) {
  MDC.put(key, value)
  try {
    action()
  } finally {
    MDC.remove(key)
  }
}

inline fun withMDC(args: Map<String, String>, action: () -> Unit) {
  args.forEach { (t, u) -> MDC.put(t, u) }
  try {
    action()
  } finally {
    args.forEach { (t, _) -> MDC.remove(t) }
  }
}
