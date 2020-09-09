import sbt._

object Dependencies {
  val versionScala213 = "2.13.3"
  val versionSpringBoot = "2.3.3.RELEASE"
  val versionAkka = "2.6.8"
  val versionAlpakka = "2.0.1"
  val versionAkkaHttp = "10.2.0"
  val versionCassandra = "4.9.0"
  val versionScalaLogging = "3.9.2"
  val versionLogback = "1.2.3"
  val versionLogstashLogback = "6.4"
  val versionAlpnAgent = "2.0.10"

  val _akkaDiscovery = "com.typesafe.akka" %% "akka-discovery" % versionAkka
  val _akkaSerializationJackson = "com.typesafe.akka" %% "akka-serialization-jackson" % versionAkka
  val _akkaProtobufV3 = "com.typesafe.akka" %% "akka-protobuf-v3" % versionAkka
  val _akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % versionAkka

  val _akkaMultiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit" % versionAkka

  val _akkaClusters = Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % versionAkka,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % versionAkka)

  val _akkaHttp = ("com.typesafe.akka" %% "akka-http" % versionAkkaHttp)
    .exclude("com.typesafe.akka", "akka-stream")
    .cross(CrossVersion.binary)

  val _alpakkaCsv =
    ("com.lightbend.akka" %% "akka-stream-alpakka-csv" % versionAlpakka)
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)

  val _alpakkaFile =
    ("com.lightbend.akka" %% "akka-stream-alpakka-file" % versionAlpakka)
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)

  val _alpakkaFtp =
    ("com.lightbend.akka" %% "akka-stream-alpakka-ftp" % versionAlpakka)
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)

  val _alpakkaSpringWeb =
    ("com.lightbend.akka" %% "akka-stream-alpakka-spring-web" % versionAlpakka)
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)
      .excludeAll(ExclusionRule("org.springframework"))

  val _cassandras = Seq("com.datastax.oss" % "java-driver-core" % versionCassandra)

  val _logs = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % versionScalaLogging,
    "ch.qos.logback" % "logback-classic" % versionLogback)

  val _logstashLogbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % versionLogstashLogback

  val _springs = Seq("org.springframework.boot" % "spring-boot-starter-web" % versionSpringBoot)

  val _alpnAgent = "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % versionAlpnAgent
}
