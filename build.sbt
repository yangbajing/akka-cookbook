import Commons._
import Dependencies._

ThisBuild / offline := true

ThisBuild / updateOptions := updateOptions.value.withCachedResolution(true).withLatestSnapshots(false)

ThisBuild / scalaVersion := versionScala213

ThisBuild / scalafmtOnCompile := true

ThisBuild / resolvers ++= Seq(Resolver.bintrayRepo("akka-fusion", "maven"), Resolver.sonatypeRepo("snapshots"))

lazy val root = Project(id = "akka-cookbook", base = file(".")).aggregate(
  book,
  actionOauth2,
  storageCassandra,
  integrationSpring,
  cookbookGrpc,
  cookbookPersistence,
  cookbookCluster,
  cookbookStreams,
  cookbookActor,
  cookbookCommon)

lazy val book =
  _project("book")
    .enablePlugins(AkkaParadoxPlugin)
    .dependsOn(
      storageCassandra,
      actionOauth2,
      integrationSpring,
      cookbookGrpc,
      cookbookPersistence,
      cookbookCluster,
      cookbookStreams,
      cookbookActor,
      cookbookCommon)
    .settings(Publishing.noPublish: _*)
    .settings(
      resolvers += Resolver.jcenterRepo,
      publish / skip := true,
      paradoxRoots := List("index.html"),
      paradoxGroups := Map("Language" -> Seq("Scala", "Java")),
      sourceDirectory in Compile in paradoxTheme := sourceDirectory.value / "main" / "paradox" / "_template",
      Compile / paradoxProperties ++= Map(
          "project.name" -> "Akka Cookbook",
          "canonical.base_url" -> "https://www.yangbajing.me/akka-cookbook",
          "github.base_url" -> s"https://github.com/yangbajing/akka-cookbook/tree/master",
          "version" -> version.value,
          "scaladoc.akka.base_url" -> "https://doc.akka.io/api/akka/2.6",
          "scaladoc.akka.http.base_url" -> "https://doc.akka.io/api/akka-http/current",
          "javadoc.akka.base_url" -> "https://doc.akka.io/japi/akka/2.6",
          "javadoc.akka.http.base_url" -> "https://doc.akka.io/japi/akka-http/current",
          "scala.version" -> scalaVersion.value,
          "scala.binary_version" -> scalaBinaryVersion.value,
          "extref.wikipedia.base_url" -> "https://en.wikipedia.org/wiki/%s",
          "alpakka.version" -> fusion.sbt.gen.BuildInfo.versionAlpakka,
          "scaladoc.akka.base_url" -> s"http://doc.akka.io/api/${fusion.sbt.gen.BuildInfo.versionAkka}",
          "algolia.docsearch.api_key" -> "bc8e6a27d54d01e7d322395f061bf539",
          "algolia.docsearch.index_name" -> "akka-cookbook",
          "akka.version" -> fusion.sbt.gen.BuildInfo.versionAkka))

lazy val cookbookGrpc = _project("cookbook-grpc")
  .enablePlugins(AkkaGrpcPlugin, JavaAgent, JavaAppPackaging)
  .dependsOn(cookbookStreams, cookbookCommon % "compile->compile;test->test")
  .settings(
    javaAgents += _alpnAgent % "runtime;test",
    mainClass in assembly := Some("greeter.GreeterApplication"),
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case PathList("io", "netty", xs @ _*)               => MergeStrategy.first
      case PathList("google", "protobuf", xs @ _*)        => MergeStrategy.first
      case PathList("com", "google", "protobuf", xs @ _*) => MergeStrategy.first
      case PathList("scalapb", xs @ _*)                   => MergeStrategy.first
      case "application.conf"                             => MergeStrategy.concat
      case "reference.conf"                               => MergeStrategy.concat
      case "module-info.class"                            => MergeStrategy.concat
      case "META-INF/io.netty.versions.properties"        => MergeStrategy.first
      case "META-INF/native/libnetty-transport-native-epoll.so" =>
        MergeStrategy.first
      case n if n.endsWith(".txt")   => MergeStrategy.concat
      case n if n.endsWith("NOTICE") => MergeStrategy.concat
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    akkaGrpcCodeGeneratorSettings += "server_power_apis",
    libraryDependencies ++= Seq(
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
        _akkaDiscovery))

lazy val actionOauth2 = _project("action-oauth2")
  .dependsOn(cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_fusionJson, _akkaPersistenceTyped) ++ _akkaClusters)

lazy val cookbookActor = _project("cookbook-actor").dependsOn(cookbookCommon % "compile->compile;test->test")

lazy val cookbookStreams = _project("cookbook-streams")
  .dependsOn(cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_alpakkaCsv, _alpakkaFtp, _alpakkaFile))

lazy val cookbookCluster = _project("cookbook-cluster")
  .dependsOn(cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_fusionJson) ++ _akkaClusters)

lazy val cookbookPersistence = _project("cookbook-persistence")
  .dependsOn(cookbookCluster, cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_akkaPersistenceTyped))

lazy val storageCassandra = _storageProject("cookbook-cassandra")
  .dependsOn(cookbookCluster, cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= _cassandras)

lazy val integrationSpring = _integrationProject("cookbook-spring")
  .dependsOn(cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_alpakkaSpringWeb) ++ _springs)

lazy val cookbookCommon = _project("cookbook-common").settings(
  libraryDependencies ++= Seq(_akkaSerializationJackson % Provided, _fusionCore) ++ _logs)

def _integrationProject(name: String) = _project(name, s"integration/$name")

def _storageProject(name: String) = _project(name, s"storage/$name")

def _project(name: String, _base: String = null) =
  Project(id = name, base = file(if (_base eq null) name else _base))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(basicSettings: _*)
    .settings(Publishing.noPublish: _*)
