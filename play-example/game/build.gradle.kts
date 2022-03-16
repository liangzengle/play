import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator

plugins {
  id("play.modular-code") version "0.1"
  id("org.jetbrains.kotlin.plugin.serialization") version Versions.Kotlin
  application
}

application {
  mainClass.set("play.example.game.AppKt")
}

task("createStartScripts", CreateStartScripts::class) {
  applicationName = "app"
  val generatorUnix = UnixStartScriptGenerator()
  val generatorWin = WindowsStartScriptGenerator()
  generatorUnix.template = resources.text.fromFile("unixStartScript2.txt")
  generatorWin.template = resources.text.fromFile("windowsStartScript.txt")
  unixStartScriptGenerator = generatorUnix
  windowsStartScriptGenerator = generatorWin
}

repositories {
  mavenLocal()
  maven("https://maven.aliyun.com/repository/public/")
  mavenCentral()
  maven("file://$rootDir/repository")
}

dependencies {
  api(project(":play-example-common"))
  api(project(":play-example-rpc-api"))
  api(project(":play-net"))
  api(project(":play-mongodb"))
  api(project(":play-mvc"))
  api(project(":play-spring"))
  api(project("::play-rsocket-rpc"))

  compileOnly(project(":play-codegen-annotations"))
  compileOnly(project(":play-codegen"))
  kapt(project(":play-codegen"))

  kapt(Deps.Hibernate.ValidatorApt)

  testImplementation(Deps.KotlinPoet.Poet)
  testImplementation(Deps.KotlinPoet.Metadata)
}


kapt {
  arguments {
    arg("controller.user-class", "play.example.game.app.module.player.Self")
    arg("controller.manager.package", "play.example.game.app")
    arg("entityCache.specialized", "false")
  }
}

modularCode {
  enabled = true
  annotation = listOf("play.example.game.app.module.ModularCode")
}
