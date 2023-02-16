object Deps {

  object Akka {
    private const val Version = Versions.Akka
    const val Actor = "com.typesafe.akka:akka-actor-typed_2.13:$Version"
    const val Cluster = "com.typesafe.akka:akka-cluster-typed_2.13:$Version"

    object Serialization {
      const val Jackson = "com.typesafe.akka:akka-serialization-jackson_2.13:$Version"
      const val Kryo = "io.altoo:akka-kryo-serialization_2.13:${Versions.AkkaKryo}"
    }
  }

  object Assertj {
    const val Core = "org.assertj:assertj-core:${Versions.Assertj}"
  }

  object Dokka {
    const val Core = "org.jetbrains.dokka:dokka-core:${Versions.Dokka}"
    const val Base = "org.jetbrains.dokka:dokka-base:${Versions.Dokka}"
    const val TestApi = "org.jetbrains.dokka:dokka-test-api:${Versions.Dokka}"
    const val TestUtils = "org.jetbrains.dokka:dokka-base-test-utils:${Versions.Dokka}"
  }

  object Hibernate {
    private const val Version = Versions.HibernateValidator
    const val Validator = "org.hibernate.validator:hibernate-validator:$Version"
    const val ValidatorApt = "org.hibernate.validator:hibernate-validator-annotation-processor:$Version"
  }

  object Eclipse {
    const val Version = Versions.EclipseCollections

    object Collections {
      const val Api = "org.eclipse.collections:eclipse-collections-api:$Version"
      const val Impl = "org.eclipse.collections:eclipse-collections:$Version"
    }
  }

  @Deprecated("remove")
  const val EclipseCollections = "org.eclipse.collections:eclipse-collections:${Versions.EclipseCollections}"

  object EL {
    const val Api = "jakarta.el:jakarta.el-api:${Versions.JakartaElApi}"
    const val Impl = "org.glassfish.expressly:expressly:${Versions.JakartaEl}"
    const val Mvel = "org.mvel:mvel2:${Versions.Mvel}"
  }

  object Jackson {
    private const val Version = Versions.Jackson
    const val DataBind = "com.fasterxml.jackson.core:jackson-databind:$Version"
    const val Kotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:$Version"
    const val Jdk8 = "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$Version"
    const val JavaTime = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$Version"
    const val Guava = "com.fasterxml.jackson.datatype:jackson-datatype-guava:$Version"
    const val Scala = "com.fasterxml.jackson.module:jackson-module-scala_2.13:$Version"
    const val EclipseCollections = "com.fasterxml.jackson.datatype:jackson-datatype-eclipse-collections:$Version"
    const val Bson = "de.undercouch:bson4jackson:${Versions.Bson4Jackson}"
  }

  object Jakarta {
    object Inject {
      const val Version = "2.0.1"
      val Api = "jakarta.inject:jakarta.inject-api:2.0.1"
    }
  }

  object JCTools {
    const val Version = "3.3.0"
    const val Core = "org.jctools:jctools-core:$Version"
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
    const val Jvm = "org.jetbrains.kotlin:kotlin-stdlib:$Version"
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

  object Ktor {
    object Client {
      const val Core = "io.ktor:ktor-client-core-jvm:${Versions.Ktor}"
      const val Cio = "io.ktor:ktor-client-cio:${Versions.Ktor}"
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
    const val Buffer = "io.netty:netty-buffer:$Version"
    const val Util = "io.netty:netty-util:$Version"
  }

  object Reactor {
    const val Core = "io.projectreactor:reactor-core:${Versions.Reactor}"
  }

  object RSocket {
    const val Version = "1.1.2"

    const val Core = "io.rsocket:rsocket-core:$Version"

    object Transport {
      const val Netty = "io.rsocket:rsocket-transport-netty:$Version"
      const val Local = "io.rsocket:rsocket-transport-local:$Version"
    }
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

  object Spring {
    const val Messaging = "org.springframework:spring-messaging:5.3.22"
  }

  object SpringBoot {
    const val Boot = "org.springframework.boot:spring-boot:${Versions.SpringBoot}"
    const val Starter = "org.springframework.boot:spring-boot-starter:${Versions.SpringBoot}"
    const val StarterLog4j2 = "org.springframework.boot:spring-boot-starter-log4j2:${Versions.SpringBoot}"
    const val StarterWebflux = "org.springframework.boot:spring-boot-starter-webflux:${Versions.SpringBoot}"
    const val StarterActuator = "org.springframework.boot:spring-boot-starter-actuator:${Versions.SpringBoot}"
    const val StarterRSocket = "org.springframework.boot:spring-boot-starter-rsocket:${Versions.SpringBoot}"
  }

  object Wire {
    private const val Version = Versions.Wire
    const val Runtime = "com.squareup.wire:wire-runtime-jvm:$Version"
    const val Schema = "com.squareup.wire:wire-schema-jvm:$Version"
    const val Compiler = "com.squareup.wire:wire-compiler:$Version"
    const val KotlinGenerator = "com.squareup.wire:wire-kotlin-generator:$Version"
    const val GrpcClient = "com.squareup.wire:wire-grpc-client:$Version"
    const val GrpcServerGenerator = "com.squareup.wire:wire-grpc-server-generator:$Version"
    const val SchameTests = "com.squareup.wire:wire-schema-tests:$Version"
  }

  const val Asm = "org.ow2.asm:asm:${Versions.Asm}"

  const val AsyncHttpClient = "org.asynchttpclient:async-http-client:${Versions.AsyncHttpClient}"

  const val AutoService = "com.google.auto.service:auto-service:${Versions.AutoService}"

  const val AutoServiceKsp = "dev.zacsweers.autoservice:auto-service-ksp:${Versions.AutoServiceKsp}"

  const val ByteBuddy = "net.bytebuddy:byte-buddy:${Versions.ByteBuddy}"

  const val Caffeine = "com.github.ben-manes.caffeine:caffeine:${Versions.Caffeine}"

  const val Checker = "org.checkerframework:checker-qual:${Versions.Checker}"

  const val ClassGraph = "io.github.classgraph:classgraph:${Versions.ClassGraph}"

  const val Guava = "com.google.guava:guava:${Versions.Guava}"

  const val Jctools = "org.jctools:jctools-core:${Versions.Jctools}"

  const val Jmh = "org.openjdk.jmh:jmh-core:${Versions.Jmh}"

  const val KotlinLogging = "io.github.microutils:kotlin-logging-jvm:${Versions.KotlinLogging}"

  const val Kryo = "com.esotericsoftware:kryo:${Versions.Kryo}"

  const val Ksp = "com.google.devtools.ksp:symbol-processing-api:${Versions.Ksp}"

  const val KtlintGradle = "org.jlleitschuh.gradle:ktlint-gradle:${Versions.Ktlint}"

  const val Logback = "ch.qos.logback:logback-classic:${Versions.Logback}"

  const val Micrometer = "io.micrometer:micrometer-core:${Versions.Micrometer}"

  const val Rxjava3 = "io.reactivex.rxjava3:rxjava:${Versions.Rxjava3}"

  const val TsConfig = "com.typesafe:config:${Versions.TsConfig}"

  const val UnsafeAccessor = "io.github.karlatemp:unsafe-accessor:${Versions.UnsafeAccessor}"
}
