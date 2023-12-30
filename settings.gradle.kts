import dev.aga.gradle.versioncatalogs.Generator.generate
import dev.aga.gradle.versioncatalogs.GeneratorConfig

pluginManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://plugins.gradle.org/m2/")
  }
}

rootProject.name = "play"

plugins {
  id("dev.aga.gradle.version-catalog-generator") version("1.0.0")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral() // must include repositories here for dependency resolution to work from settings
  }
  versionCatalogs {
    generate("log4jLibs") { // the name of the generated catalog
      from(toml("log4j-bom")) // name of the bom library in the version catalog
      aliasPrefixGenerator = GeneratorConfig.NO_PREFIX
    }
  }
}

fun includeProject(name: String, parentDir: String) {
  includeProject(name, parentDir, name)
}

fun includeProject(name: String, parentDir: String?, dir: String?) {
  include(name)
  val path = StringBuilder()
  if (parentDir != null) {
    path.append(parentDir).append("/")
  }
  if (dir != null) {
    path.append(dir)
  } else {
    path.append(name)
  }
  project(":$name").projectDir = file(path.toString())
}

include("play-benchmark")
include("play-core")
include("play-res")
include("play-entity")
include("play-db")
include("play-mongodb")
include("play-net")
include("play-mvc")
include("play-akka")
include("play-scala-compact")
include("play-eclipse-collectionx")
include("play-spring")
include("play-kryo")
include("play-dokka")
include("play-wire")
include("play-redis")
include("play-util")
include("play-extra")
include("play-framework")

includeProject("play-rsocket-common", "play-rsocket")
includeProject("play-rsocket-core", "play-rsocket")
includeProject("play-rsocket-broker", "play-rsocket")
includeProject("play-rsocket-client", "play-rsocket")
includeProject("play-rsocket-broker-spring", "play-rsocket")
includeProject("play-rsocket-broker-server", "play-rsocket")
includeProject("play-rsocket-client-spring", "play-rsocket")
includeProject("play-rsocket-core-spring", "play-rsocket")

includeProject("play-codegen-annotations", "play-codegen", "annotations")
includeProject("play-codegen-common", "play-codegen", "common")
includeProject("play-codegen-controller", "play-codegen", "controller")
includeProject("play-codegen-entity", "play-codegen", "entity")
includeProject("play-codegen-enumeration", "play-codegen", "enumeration")
includeProject("play-codegen-resource", "play-codegen", "resource")
includeProject("play-codegen-rpc", "play-codegen", "rpc")

include("play-modular-code")
project(":play-modular-code").projectDir = file("./play-plugins/play-modular-code")
