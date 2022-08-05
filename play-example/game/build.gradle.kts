import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator

plugins {
  id("play.modular-code") version "0.1"
  id("org.jetbrains.kotlin.plugin.serialization") version Versions.Kotlin
  application
}

apply(plugin = "kotlin-kapt")

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
//  mavenLocal()
  maven("https://maven.aliyun.com/repository/public/")
  mavenCentral()
  maven("file://$rootDir/repository")
}

val kapt by configurations

dependencies {
  api(project(":play-example-common"))
  api(project(":play-example-rpc-api"))
  api(project(":play-net"))
  api(project(":play-entity"))
  api(project(":play-db"))
  api(project(":play-mongodb"))
  api(project(":play-mvc"))
  api(project(":play-spring"))
  api(project("::play-rsocket-rpc"))

  api(project("::play-codec"))
  implementation(Deps.Kotlinx.Serialization.Protobuf)

  compileOnly(project(":play-codegen-annotations"))
  compileOnly(project(":play-codegen"))
  ksp(project(":play-codegen"))
  ksp(Deps.AutoServiceKsp)

  compileOnly(Deps.Hibernate.ValidatorApt)
  kapt(Deps.Hibernate.ValidatorApt)

  testImplementation(Deps.KotlinPoet.Poet)
  testImplementation(Deps.KotlinPoet.Metadata)
}

modularCode {
  enabled = true
  annotation = listOf("play.example.game.app.module.ModularCode")
}

kotlin {
  sourceSets.main {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
  }
  sourceSets.test {
    kotlin.srcDir("build/generated/ksp/test/kotlin")
  }
}
