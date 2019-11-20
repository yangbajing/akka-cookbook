import Commons._
import Dependencies._

ThisBuild / scalaVersion := versionScala213

ThisBuild / scalafmtOnCompile := true

lazy val root = Project(id = "akka-cookbook", base = file(".")).aggregate(
  book,
  cookbookActor,
  cookbookStreams,
  cookbookCluster,
  cookbookPersistence,
  storageCassandra,
  integrationSpring,
  cookbookCommon)

lazy val book = _project("book")
  .enablePlugins(ParadoxMaterialThemePlugin)
  .dependsOn(cookbookActor, cookbookStreams, cookbookCommon)
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

lazy val cookbookActor = _project("cookbook-actor").dependsOn(cookbookCommon % "compile->compile;test->test")

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
