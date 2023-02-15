import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://plugins.gradle.org/m2/")
  }

  dependencies {
    classpath(Deps.Kotlin.Gradle)
//    classpath(Deps.Kotlin.AllOpen)
    classpath(Deps.KtlintGradle)
  }
}

plugins {
  `java-library`
  id("com.google.devtools.ksp") version Versions.Ksp apply false
}

subprojects {
  repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
  }
  apply(plugin = "java")
  apply(plugin = "kotlin")
  apply(plugin = "org.jlleitschuh.gradle.ktlint-idea")
//  apply(plugin = "kotlin-spring")
  apply(plugin = "project-report")
  apply(plugin = "com.google.devtools.ksp")
  apply(from = "$rootDir/gradle/publish.gradle")

  configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    debug.set(true)
    verbose.set(true)
  }

  group = "me.play"
  version = "1.0-SNAPSHOT"

  val api by configurations
  val testImplementation by configurations

  dependencies {
    api(Deps.Kotlin.Jvm)
    api(Deps.Kotlin.Reflect)
    api(Deps.AutoService)

    testImplementation(Deps.Junit.JupiterEngine)
    testImplementation(Deps.Junit.JupiterApi)
    testImplementation(Deps.Junit.Jupiter)
  }

  tasks {
    "test"(Test::class) {
      useJUnitPlatform()
      testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showExceptions = true
        showCauses = true
        showStackTraces = true
      }
    }
  }

  val javaVersion = JavaVersion.VERSION_17
  java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  val kotlinCompilerArgs = listOf(
    "-Xallow-result-return-type",
//    "-XXLanguage:+InlineClasses",
    "-opt-in=kotlin.RequiresOptIn",
//    "-opt-in=kotlin.ExperimentalUnsignedTypes",
    "-opt-in=kotlin.time.ExperimentalTime",
//    "-opt-in=kotlin.contracts.ExperimentalContracts",
//    "-opt-in=kotlin.experimental.ExperimentalTypeInference",
//    "-opt-in=kotlin.io.path.ExperimentalPathApi",
    "-opt-in=kotlin.ExperimentalStdlibApi",
    "-Xjvm-default=all",
    "-Xstring-concat=indy-with-constants",
    "-Xcontext-receivers"
  )

  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion.toString()
    kotlinOptions.javaParameters = true
    kotlinOptions.freeCompilerArgs = kotlinCompilerArgs
  }

  tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
  }

  sourceSets.main {
    java.srcDir("src/main/kotlin")
  }

  sourceSets.test {
    java.srcDir("src/test/kotlin")
  }

  tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
}
