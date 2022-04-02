import org.jetbrains.kotlin.gradle.tasks.*

dependencies {
  implementation(project(":play-codegen-annotations"))
  implementation(Deps.Kotlin.Reflect)
  implementation(Deps.KotlinPoet.Poet)
  implementation(Deps.KotlinPoet.Metadata)
  implementation(Deps.KotlinPoet.Ksp)
  implementation(Deps.Ksp)
  ksp(Deps.AutoServiceKsp)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-Xopt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview"
  kotlinOptions.freeCompilerArgs += "-Xopt-in=com.squareup.kotlinpoet.DelicateKotlinPoetApi"
  kotlinOptions.freeCompilerArgs += "-Xopt-in=com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview"
  kotlinOptions.freeCompilerArgs += "-Xopt-in=com.google.devtools.ksp.KspExperimental"
}
