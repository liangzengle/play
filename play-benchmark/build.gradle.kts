apply(plugin = "org.jetbrains.kotlin.kapt")

val kapt by configurations

dependencies {
  implementation("org.openjdk.jmh:jmh-core:${libs.versions.jmh.get()}")
  implementation("org.openjdk.jmh:jmh-generator-annprocess:${libs.versions.jmh.get()}")
  kapt("org.openjdk.jmh:jmh-generator-annprocess:${libs.versions.jmh.get()}")
}
