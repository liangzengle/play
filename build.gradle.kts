import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-library`
  id("com.google.devtools.ksp") version libs.versions.ksp.get() apply false
  alias(libs.plugins.ktlint) apply false
  kotlin("jvm") version libs.versions.kotlin.asProvider().get() apply false
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
    api(rootProject.libs.kotlin.jvm)
    api(rootProject.libs.kotlin.reflect)
    api(rootProject.libs.auto.service)

    testImplementation(rootProject.libs.bundles.junit)
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

  val javaVersion = JavaVersion.VERSION_21
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
