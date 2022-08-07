dependencies {
  api(Deps.Akka.Actor)
  api(Deps.Akka.Cluster)
  api(Deps.Akka.Serialization.Jackson)
  api(project(":play-scala-compact"))
  compileOnly(project(":play-core"))
}
