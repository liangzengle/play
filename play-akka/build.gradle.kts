dependencies {
  api(Deps.Akka.Actor)
  api(Deps.Akka.Cluster)
  api(project(":play-scala-compact"))
  compileOnly(project(":play-core"))
  compileOnly(project(":play-kryo"))
  compileOnly(Deps.Akka.Serialization.Jackson)
  compileOnly(Deps.Akka.Serialization.Kryo)
}
