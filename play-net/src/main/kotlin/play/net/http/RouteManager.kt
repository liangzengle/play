package play.net.http

import io.vavr.control.Option

abstract class RouteManager {
  abstract fun findAction(uri: String): Option<Route>
}
