import Commons._
import Dependencies._

ThisBuild / scalaVersion := versionScala213

ThisBuild / scalafmtOnCompile := true

lazy val root = Project(id = "akka-cookbook", base = file(".")).aggregate(
  book,
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
    .enablePlugins(ParadoxMaterialThemePlugin)
    .dependsOn(
      storageCassandra,
      integrationSpring,
      cookbookGrpc,
      cookbookPersistence,
      cookbookCluster,
      cookbookStreams,
      cookbookActor,
      cookbookCommon)
    .settings(Publishing.noPublish: _*)
    .settings(
      Compile / paradoxMaterialTheme ~= {
        _.withLanguage(java.util.Locale.SIMPLIFIED_CHINESE)
          .withRepository(uri("https://github.com/yangbajing/akka-cookbook"))
          .withSocial(
            uri("https://yangbajing.github.io/akka-cookbook/"),
            uri("https://github.com/yangbajing/akka-cookbook"),
            uri("https://weibo.com/yangbajing"))
      },
      paradoxProperties ++= Map(
          "github.base_url" -> s"https://github.com/yangbajing/akka-cookbook/tree/${version.value}",
          "version" -> version.value,
          "scala.version" -> scalaVersion.value,
          "scala.binary_version" -> scalaBinaryVersion.value,
          "alpakka.version" -> versionAlpakka,
          "scaladoc.akka.base_url" -> s"http://doc.akka.io/api/$versionAkka",
          "akka.version" -> versionAkka))

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
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"))

lazy val cookbookActor = _project("cookbook-actor").dependsOn(
  cookbookCommon % "compile->compile;test->test")

lazy val cookbookStreams = _project("cookbook-streams")
  .dependsOn(cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_alpakkaCsv, _alpakkaFtp, _alpakkaFile))

lazy val cookbookCluster = _project("cookbook-cluster")
  .dependsOn(cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= _akkaClusters)

lazy val cookbookPersistence = _project("cookbook-persistence")
  .dependsOn(cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_akkaPersistenceTyped))

lazy val storageCassandra = _storageProject("cookbook-cassandra")
  .dependsOn(cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= _cassandras)

lazy val integrationSpring = _integrationProject("cookbook-spring")
  .dependsOn(cookbookCommon % "compile->compile;test->test")
  .settings(libraryDependencies ++= Seq(_alpakkaSpringWeb) ++ _springs)

lazy val cookbookCommon = _project("cookbook-common").settings(
  libraryDependencies ++= Seq(
      _uuidGenerator,
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      _akkaSerializationJackson % Provided,
      _scalaCollectionCompat,
      _scalaJava8Compat,
      _akkaTypedTestkit % Test,
      _akkaStreamTestkit % Test,
      _scalatest % Test) ++ _akkas ++ _logs)

def _integrationProject(name: String) = _project(name, s"integration/$name")

def _storageProject(name: String) = _project(name, s"storage/$name")

def _project(name: String, _base: String = null) =
  Project(id = name, base = file(if (_base eq null) name else _base))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(basicSettings: _*)
    .settings(Publishing.noPublish: _*)
