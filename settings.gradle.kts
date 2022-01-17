enableFeaturePreview("VERSION_CATALOGS")
pluginManagement {
  repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://plugins.gradle.org/m2/")
    maven("file://$rootDir/repository")
  }
}

rootProject.name = "play"

include("play-core")
include("play-res")
include("play-entity")
include("play-db")
include("play-mongodb")
include("play-net")
include("play-mvc")
include("play-codegen")
include("play-codegen-annotations")
include("play-akka")
include("play-scala")
include("play-scala-compact")
include("play-eclipse-collectionx")
include("play-spring")
include("play-boot")
include("play-primitive-collection")
include("play-rsocket-rpc")

include("play-modular-code")
project(":play-modular-code").projectDir = file("./play-plugins/play-modular-code")

include("play-example-common")
project(":play-example-common").projectDir = file("./play-example/common")
include("play-example-game")
project(":play-example-game").projectDir = file("./play-example/game")
include("play-example-robot")
project(":play-example-robot").projectDir = file("./play-example/robot")

include("play-example-rpc-broker")
project(":play-example-rpc-broker").projectDir = file("./play-example/rpc-broker")
include("play-example-rpc-api")
project(":play-example-rpc-api").projectDir = file("./play-example/rpc-api")
include("play-example-rpc-test")
project(":play-example-rpc-test").projectDir = file("./play-example/rpc-test")
