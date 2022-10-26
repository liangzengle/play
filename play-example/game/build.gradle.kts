import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator

plugins {
  id("play.modular-code") version "0.1"
  application
  java
  idea
}

//apply(plugin = "kotlin-kapt")

tasks.jar {
  val resourceMain = "$buildDir/resources/main"
  val metaInf = "$buildDir/resources/main/META-INF"
  exclude { file ->
    val path = file.file.toPath()
    path.startsWith(resourceMain) && !path.startsWith(metaInf)
  }
}

application {
  mainClass.set("play.example.game.ContainerApp")
  applicationDistribution.into("conf") {
    from("src/main/conf")
  }
  applicationDefaultJvmArgs = listOf(
    "--add-opens",
    "java.base/java.lang=ALL-UNNAMED",
    "-Djdk.attach.allowAttachSelf=true"
  )
}

tasks.startScripts {
  unixStartScriptGenerator = plugin.PlayUnixStartScriptGenerator()
  windowsStartScriptGenerator = plugin.PlayWindowsStartScriptGenerator()
}

task("createStartScripts", CreateStartScripts::class) {
  applicationName = "game"
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

//val kapt by configurations

dependencies {
  api(project(":play-example-common"))
  api(project(":play-example-rpc-api"))
  api(project("::play-example-protos"))
  api(project(":play-net"))
  api(project(":play-entity"))
  api(project(":play-db"))
  api(project(":play-mongodb"))
  api(project(":play-mvc"))
  api(project(":play-spring"))

  ksp(Deps.AutoServiceKsp)
  compileOnly(project(":play-codegen-annotations"))
  compileOnly(project(":play-codegen-controller"))
  ksp(project(":play-codegen-controller"))
  compileOnly(project(":play-codegen-entity"))
  ksp(project(":play-codegen-entity"))
  compileOnly(project(":play-codegen-resource"))
  ksp(project(":play-codegen-resource"))
  compileOnly(project(":play-codegen-enumeration"))
  ksp(project(":play-codegen-enumeration"))
  compileOnly(project(":play-codegen-rpc"))
  ksp(project(":play-codegen-rpc"))


//  compileOnly(Deps.Hibernate.ValidatorApt)
//  kapt(Deps.Hibernate.ValidatorApt)

  testImplementation(Deps.KotlinPoet.Poet)
  testImplementation(Deps.KotlinPoet.Metadata)
  testImplementation("com.tngtech.archunit:archunit:1.0.0-rc1")
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

sourceSets.main {
  resources {
    srcDir("src/main/conf")
  }
}
