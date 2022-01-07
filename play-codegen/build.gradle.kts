import org.jetbrains.kotlin.gradle.tasks.*

dependencies {
  compileOnly(project(":play-codegen-annotations"))
  implementation(Deps.Kotlin.Reflect)
  implementation(Deps.KotlinPoet.Poet)
  implementation(Deps.KotlinPoet.Metadata)
  implementation(Deps.KotlinPoet.Ksp)
  implementation(Deps.Ksp)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-Xopt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview"
}
