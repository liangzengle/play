package play.example.scene.base

import com.google.auto.service.AutoService
import com.google.inject.Module
import play.inject.guice.GuiceModule

/**
 *
 * @author LiangZengle
 */
@AutoService(Module::class)
class SceneGuiceModule : GuiceModule() {
  override fun configure() {
  }
}
