import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
  implementation(project(":play-codegen-common"))
  implementation(project(":play-rsocket-common"))
  ksp(Deps.AutoServiceKsp)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-opt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview"
  kotlinOptions.freeCompilerArgs += "-opt-in=com.squareup.kotlinpoet.DelicateKotlinPoetApi"
  kotlinOptions.freeCompilerArgs += "-opt-in=com.google.devtools.ksp.KspExperimental"
}
