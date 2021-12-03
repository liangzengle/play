package play.example.game.app.module.maintask.res

import play.example.game.app.module.task.res.AbstractTaskResource
import play.example.game.app.module.task.res.AbstractTaskResourceExtension
import play.res.ExtensionKey
import play.res.ResourcePath

/**
 * 主线任务配置
 */
@ResourcePath("MainTask")
public class MainTaskResource : AbstractTaskResource(), ExtensionKey<MainTaskResourceExtension>

class MainTaskResourceExtension(elements: List<MainTaskResource>) : AbstractTaskResourceExtension<MainTaskResource>(elements)
