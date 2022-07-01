object Deps {

  object Akka {
    private const val Version = Versions.Akka
    const val Actor = "com.typesafe.akka:akka-actor-typed_2.13:$Version"
    const val Cluster = "com.typesafe.akka:akka-cluster-typed_2.13:$Version"
    const val SerializationJackson = "com.typesafe.akka:akka-serialization-jackson_2.13:$Version"
  }

  object AlibabaRsocket {
    val Core = "com.alibaba.rsocket:alibaba-rsocket-core:${Versions.AlibabaRsocket}"
    val Broker = "com.alibaba.rsocket:alibaba-broker-spring-boot-starter:${Versions.AlibabaRsocket}"
    val Client = "com.alibaba.rsocket:alibaba-rsocket-spring-boot-starter:${Versions.AlibabaRsocket}"
  }

  object Hibernate {
    private const val Version = Versions.HibernateValidator
    const val Validator = "org.hibernate.validator:hibernate-validator:$Version"
    const val ValidatorApt = "org.hibernate.validator:hibernate-validator-annotation-processor:$Version"
  }

  object EL {
    const val Api = "jakarta.el:jakarta.el-api:${Versions.JakartaElApi}"
    const val Impl = "org.glassfish:jakarta.el:${Versions.JakartaEl}"
    const val Mvel = "org.mvel:mvel2:${Versions.Mvel}"
  }

  object Jackson {
    private const val Version = Versions.Jackson
    const val Kotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:$Version"
    const val Jdk8 = "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$Version"
    const val Guava = "com.fasterxml.jackson.datatype:jackson-datatype-guava:$Version"
    const val Scala = "com.fasterxml.jackson.module:jackson-module-scala_2.13:$Version"
    const val EclipseCollections = "com.fasterxml.jackson.datatype:jackson-datatype-eclipse-collections:$Version"
    const val Bson = "de.undercouch:bson4jackson:${Versions.Bson4Jackson}"
  }

  object Junit {
    private const val Version = Versions.Junit
    const val JupiterEngine = "org.junit.jupiter:junit-jupiter-engine:$Version"
    const val JupiterApi = "org.junit.jupiter:junit-jupiter-api:$Version"
    const val Jupiter = "org.junit.jupiter:junit-jupiter:$Version"
  }

  object Kotlin {
    private const val Version = Versions.Kotlin
    const val Compiler = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$Version"
    const val Jvm = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$Version"
    const val Reflect = "org.jetbrains.kotlin:kotlin-reflect:$Version"
    const val Gradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:$Version"
    const val GradleApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$Version"
    const val AllOpen = "org.jetbrains.kotlin:kotlin-allopen:$Version"
  }

  object KotlinPoet {
    private const val Version = Versions.KotlinPoet
    const val Poet = "com.squareup:kotlinpoet:$Version"
    const val Metadata = "com.squareup:kotlinpoet-metadata:$Version"
    const val Ksp = "com.squareup:kotlinpoet-ksp:$Version"
  }

  @Deprecated("")
  const val KotlinPoetDeprecated = "com.squareup:kotlinpoet:${Versions.KotlinPoet}"

  object Kotlinx {
    object Serialization {
      const val Core = "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${Versions.KotlinxSerialization}"
      const val Protobuf = "org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:${Versions.KotlinxSerialization}"
    }
  }

  object Log4j {
    private const val Version = Versions.Log4j
    const val Api = "org.apache.logging.log4j:log4j-api:$Version"
    const val Core = "org.apache.logging.log4j:log4j-core:$Version"
    const val Slf4j = "org.apache.logging.log4j:log4j-slf4j-impl:$Version"
    const val Jul = "org.apache.logging.log4j:log4j-jul:$Version"
  }

  object MongoDB {
    const val Driver = "org.mongodb:mongodb-driver-reactivestreams:${Versions.MongoDB}"
  }

  object Netty {
    private const val Version = Versions.Netty
    const val All = "io.netty:netty-all:$Version"
    const val CodecHttp = "io.netty:netty-codec-http:$Version"
    const val Epoll = "io.netty:netty-transport-native-epoll:$Version"
    const val Handler = "io.netty:netty-handler:$Version"
  }

  object Reactor {
    const val Core = "io.projectreactor:reactor-core:${Versions.Reactor}"
  }

  object Slf4j {
    private const val Version = Versions.Slf4j
    const val Api = "org.slf4j:slf4j-api:$Version"
    const val Simple = "org.slf4j:slf4j-simple:$Version"
  }

  object Scala {
    const val Lang = "org.scala-lang:scala-library:${Versions.Scala}"
    const val Java8 = "org.scala-lang.modules:scala-java8-compat_2.13:${Versions.ScalaJava8Compact}"
  }

  object SpringBoot {
    const val Starter = "org.springframework.boot:spring-boot-starter:${Versions.SpringBoot}"
    const val StarterWebflux = "org.springframework.boot:spring-boot-starter-webflux:${Versions.SpringBoot}"
    const val StarterActuator = "org.springframework.boot:spring-boot-starter-actuator:${Versions.SpringBoot}"
  }

  const val Asm = "org.ow2.asm:asm:${Versions.Asm}"

  const val AsyncHttpClient = "org.asynchttpclient:async-http-client:${Versions.AsyncHttpClient}"

  const val AutoService = "com.google.auto.service:auto-service:${Versions.AutoService}"

  const val AutoServiceKsp = "dev.zacsweers.autoservice:auto-service-ksp:${Versions.AutoServiceKsp}"

  const val Brave = "io.zipkin.brave:brave:${Versions.Brave}"

  const val ByteBuddy = "net.bytebuddy:byte-buddy:${Versions.ByteBuddy}"

  const val Caffeine = "com.github.ben-manes.caffeine:caffeine:${Versions.Caffeine}"

  const val Checker = "org.checkerframework:checker-qual:${Versions.Checker}"

  const val ClassGraph = "io.github.classgraph:classgraph:${Versions.ClassGraph}"

  const val EclipseCollections = "org.eclipse.collections:eclipse-collections:${Versions.EclipseCollections}"

  const val FastUtil = "it.unimi.dsi:fastutil-core:${Versions.FastUtil}"

  const val Guava = "com.google.guava:guava:${Versions.Guava}"

  const val Jctools = "org.jctools:jctools-core:${Versions.Jctools}"

  const val KotlinLogging = "io.github.microutils:kotlin-logging-jvm:${Versions.KotlinLogging}"

  const val Ksp = "com.google.devtools.ksp:symbol-processing-api:${Versions.Ksp}"

  const val KtlintGradle = "org.jlleitschuh.gradle:ktlint-gradle:${Versions.Ktlint}"

  const val Logback = "ch.qos.logback:logback-classic:${Versions.Logback}"

  const val Micrometer = "io.micrometer:micrometer-core:${Versions.Micrometer}"

  const val Rxjava3 = "io.reactivex.rxjava3:rxjava:${Versions.Rxjava3}"

  const val TsConfig = "com.typesafe:config:${Versions.TsConfig}"

  const val UnsafeAccessor = "io.github.karlatemp:unsafe-accessor:${Versions.UnsafeAccessor}"
}
