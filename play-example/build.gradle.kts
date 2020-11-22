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
  id("play.modular-code") version "0.1"
  id("org.jetbrains.dokka") version "1.4.10.2"
  id("com.squareup.wire")
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
  implementation(project(":play-db-mysql-nosql"))
//  implementation(project(":play-scala-compact"))
//    implementation project(":play-db-mongo")
//    compileOnly project(path: ":play-codegen", configuration: "default")
  compileOnly(project(":play-codegen"))
  kapt(project(":play-codegen"))

  implementation("mysql:mysql-connector-java:8.0.20")
  implementation("org.jctools:jctools-core:3.0.0")
  implementation("com.squareup.wire:wire-runtime:3.3.0")
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

wire {
  kotlin {}
}

modularCode {
  annotation = listOf("play.example.module.ModularCode")
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
