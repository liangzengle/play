package play.example.game.app.module.servertask.res

import play.example.game.app.module.task.res.AbstractTaskResource
import play.res.ExtensionKey
import play.res.ResourceExtension
import play.util.collection.toImmutableSet

class ServerTaskResource : AbstractTaskResource(), ExtensionKey<ServerTaskResourceExtension> {

}

class ServerTaskResourceExtension(elements: List<ServerTaskResource>) :
  ResourceExtension<ServerTaskResource>(elements) {
  val targetTypes = elements.asSequence().flatMap { it.targets }.map { it.type }.toImmutableSet()
}
