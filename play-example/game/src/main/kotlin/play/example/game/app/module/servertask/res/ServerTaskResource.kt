package play.example.game.app.module.servertask.res

import play.example.game.app.module.task.res.AbstractTaskResource
import play.example.game.app.module.task.res.TaskResourceExtension
import play.res.ExtensionKey

class ServerTaskResource : AbstractTaskResource(), ExtensionKey<ServerTaskResourceExtension>

class ServerTaskResourceExtension(elements: List<ServerTaskResource>) :
  TaskResourceExtension<ServerTaskResource>(elements)
