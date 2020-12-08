import org.jetbrains.dokka.DokkaConfiguration.SerializationFormat
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.gradle.DokkaTask

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10")
  }
}

plugins {
  idea
  application
  kotlin("plugin.serialization") version "1.4.20"
  id("org.jetbrains.dokka") version "1.4.10.2"
  id("play.modular-code") version "0.1"
}

repositories {
  mavenLocal()
  mavenCentral()
  maven("https://jitpack.io")
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.7.0"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("org.junit.jupiter:junit-jupiter-engine")
  testImplementation("org.mockito:mockito-inline:3.2.0")
  testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23")

  testImplementation("com.squareup:kotlinpoet:1.7.1")

  implementation(project(":play-mvc"))
  implementation(project(":play-akka"))
  implementation(project(":play-net"))
  implementation(project(":play-config"))
  // use mysql
  implementation(project(":play-db-mysql-nosql"))
  implementation("mysql:mysql-connector-java:8.0.22")
  // use mongodb
//    implementation project(":play-db-mongo")

  compileOnly(project(":play-codegen"))
  kapt(project(":play-codegen"))
  implementation("org.jctools:jctools-core:3.0.0")

  implementation("io.github.esentsov:kotlin-visibility:1.1.0")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.0.1")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

kapt {
  arguments {
    arg("controller.user-class", "play.example.module.player.Self")
    arg("entityCache.specialized", "true")
  }
}

modularCode {
  enabled = true
  annotation = "play.example.module.ModularCode"
}

tasks.register<DokkaTask>("dokkaJson") {
  dependencies {
    plugins("dokka:dokka-json:0.1")
  }
  pluginsConfiguration.set(
    listOf(
      PluginConfigurationImpl(
        "dokka.json.JsonPlugin",
        SerializationFormat.JSON,
        """{"postfixes": ["Log","LogSource","ErrorCode"]}"""
      )
    )
  )
}
