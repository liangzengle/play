//import com.google.protobuf.gradle.*

plugins {
  id("com.squareup.wire") version Versions.Wire
}

dependencies {
//  protobuf(project(":play-example-protos"))
//  api(Deps.Protobuf.KotlinLite)
//  api(Deps.PBAndK.JVM)
//  api(Deps.Wire.Runtime)
  compileOnly(Deps.Wire.Schema)
}

//sourceSets.main {
//  proto.srcDir("src/main/protobuf")
//}

tasks.getByName("compileJava").enabled = false

wire {
  sourcePath {
    srcDir("src/main/protobuf")
  }
  kotlin {}
}
