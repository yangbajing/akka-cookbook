import sbt._

object Dependencies {
  val versionScala213 = "2.13.7"
  val versionSpringBoot = "2.3.12.RELEASE"
  val versionScalatest = "3.1.4"
  val versionAkka = "2.6.18"
  val versionAlpakka = "3.0.4"
  val versionAkkaHttp = "10.2.7"
  val versionCassandra = "4.13.0"
  val versionScalaLogging = "3.9.4"
  val versionLogback = "1.2.10"
  val versionAlpnAgent = "2.0.10"

  val _scalatest = "org.scalatest" %% "scalatest" % versionScalatest

  val _akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % versionAkka
  val _akkaStreamTyped = "com.typesafe.akka" %% "akka-stream-typed" % versionAkka
  val _akkaDiscovery = "com.typesafe.akka" %% "akka-discovery" % versionAkka
  val _akkaSerializationJackson = "com.typesafe.akka" %% "akka-serialization-jackson" % versionAkka
  val _akkaProtobufV3 = "com.typesafe.akka" %% "akka-protobuf-v3" % versionAkka
  val _akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % versionAkka
  val _akkaTypedTestkit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % versionAkka
  val _akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % versionAkka

  val _akkas =
    Seq("com.typesafe.akka" %% "akka-slf4j" % versionAkka, _akkaActorTyped, _akkaStreamTyped)
      .map(_.exclude("org.scala-lang.modules", "scala-java8-compat").cross(CrossVersion.binary))

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

  val _javaUuidGenerator = "com.fasterxml.uuid" % "java-uuid-generator" % "4.0.1"

  val _logs = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % versionScalaLogging,
    "ch.qos.logback" % "logback-classic" % versionLogback)

  val _springs = Seq("org.springframework.boot" % "spring-boot-starter-web" % versionSpringBoot)

  val _alpnAgent = "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % versionAlpnAgent

  val _guava = "com.google.guava" % "guava" % "31.0.1-jre"
}
