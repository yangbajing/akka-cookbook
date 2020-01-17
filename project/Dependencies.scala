import sbt._
import fusion.sbt.gen.BuildInfo

object Dependencies {
  val versionScala212 = "2.12.10"
  val versionScala213 = "2.13.1"
  val versionSpringBoot = "2.1.10.RELEASE"

  val _fusionCore = "com.akka-fusion" %% "fusion-core" % BuildInfo.version
  val _fusionJson = "com.akka-fusion" %% "fusion-json" % BuildInfo.version
  val _fusionTestkit = "com.akka-fusion" %% "fusion-testkit" % BuildInfo.version

  val _akkaDiscovery = "com.typesafe.akka" %% "akka-discovery" % BuildInfo.versionAkka
  val _akkaSerializationJackson = "com.typesafe.akka" %% "akka-serialization-jackson" % BuildInfo.versionAkka
  val _akkaProtobufV3 = "com.typesafe.akka" %% "akka-protobuf-v3" % BuildInfo.versionAkka
  val _akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % BuildInfo.versionAkka

  val _akkaMultiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit" % BuildInfo.versionAkka

  val _akkaClusters = Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % BuildInfo.versionAkka,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % BuildInfo.versionAkka)

  val _akkaHttp = ("com.typesafe.akka" %% "akka-http" % BuildInfo.versionAkkaHttp)
    .exclude("com.typesafe.akka", "akka-stream")
    .cross(CrossVersion.binary)

  val _alpakkaCsv =
    ("com.lightbend.akka" %% "akka-stream-alpakka-csv" % BuildInfo.versionAlpakka)
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)

  val _alpakkaFile =
    ("com.lightbend.akka" %% "akka-stream-alpakka-file" % BuildInfo.versionAlpakka)
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)

  val _alpakkaFtp =
    ("com.lightbend.akka" %% "akka-stream-alpakka-ftp" % BuildInfo.versionAlpakka)
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)

  val _alpakkaSpringWeb =
    ("com.lightbend.akka" %% "akka-stream-alpakka-spring-web" % BuildInfo.versionAlpakka)
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)
      .excludeAll(ExclusionRule("org.springframework"))

  val _cassandras = Seq("com.datastax.oss" % "java-driver-core" % BuildInfo.versionCassandra)

  val _logs = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % BuildInfo.versionScalaLogging,
    "ch.qos.logback" % "logback-classic" % BuildInfo.versionLogback)

  val _logstashLogbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % BuildInfo.versionLogstashLogback

  val _springs = Seq("org.springframework.boot" % "spring-boot-starter-web" % versionSpringBoot)

  val _alpnAgent = "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % BuildInfo.versionAlpnAgent
}
