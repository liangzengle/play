import org.jetbrains.kotlin.gradle.tasks.*

dependencies {
  api(project(":play-codegen-annotations"))
  api(libs.kotlin.reflect)
  api(libs.kotlinpoet.asProvider())
  api(libs.kotlinpoet.metadata)
  api(libs.kotlinpoet.ksp)
  api(libs.ksp)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-opt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview"
  kotlinOptions.freeCompilerArgs += "-opt-in=com.squareup.kotlinpoet.DelicateKotlinPoetApi"
  kotlinOptions.freeCompilerArgs += "-opt-in=com.google.devtools.ksp.KspExperimental"
}
