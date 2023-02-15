import org.jetbrains.kotlin.gradle.tasks.*

dependencies {
  api(project(":play-codegen-annotations"))
  api(Deps.Kotlin.Reflect)
  api(Deps.KotlinPoet.Poet)
  api(Deps.KotlinPoet.Metadata)
  api(Deps.KotlinPoet.Ksp)
  api(Deps.Ksp)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-opt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview"
  kotlinOptions.freeCompilerArgs += "-opt-in=com.squareup.kotlinpoet.DelicateKotlinPoetApi"
  kotlinOptions.freeCompilerArgs += "-opt-in=com.google.devtools.ksp.KspExperimental"
}
