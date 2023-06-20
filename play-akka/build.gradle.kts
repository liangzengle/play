dependencies {
  api(libs.akka.actor)
  api(libs.akka.cluster)
  api(project(":play-scala-compact"))
  compileOnly(project(":play-core"))
  compileOnly(project(":play-kryo"))
  compileOnly(libs.akka.jackson)
  compileOnly(libs.akka.kryo)
}
