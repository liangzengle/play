apply(plugin = "org.jetbrains.kotlin.kapt")

val kapt by configurations

dependencies {
  implementation("org.openjdk.jmh:jmh-core:${Versions.Jmh}")
  implementation("org.openjdk.jmh:jmh-generator-annprocess:${Versions.Jmh}")
  kapt("org.openjdk.jmh:jmh-generator-annprocess:${Versions.Jmh}")
}
