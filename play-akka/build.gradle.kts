dependencies {
    api(Deps.Akka.Actor)
    api(Deps.Akka.Cluster)
    api(Deps.Akka.SerializationJackson)
    api(project(":play-scala-compact"))
    compileOnly(project(":play-core"))
}
