import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderLicense
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.headerLicense
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys.assemblyMergeStrategy
import sbtassembly.MergeStrategy
import sbtassembly.PathList

object Commons {
  def basicSettings =
    Seq(
      organization := "me.yangbajing",
      organizationName := "yangbajing.me",
      organizationHomepage := Some(url("https://github.com/yangbajing")),
      homepage := Some(url("https://yangbajing.github.io/akka-cookbook")),
      startYear := Some(2019),
      licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
      headerLicense := Some(HeaderLicense.ALv2("2019", "yangbajing.me")),
      scalacOptions ++= Seq(
          "-encoding",
          "UTF-8", // yes, this is 2 args
          "-feature",
          "-deprecation",
          "-unchecked",
          //"-Yno-adapted-args", //akka-http heavily depends on adapted args and => Unit implicits break otherwise
          //"-Ypartial-unification",
          "-Ywarn-dead-code",
          //"-Yrangepos", // required by SemanticDB compiler plugin
          //"-Ywarn-unused-import", // required by `RemoveUnused` rule
          "-Xlint"),
      javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
      javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),
      shellPrompt := { s =>
        Project.extract(s).currentProject.id + " > "
      },
      resolvers += Resolver.bintrayRepo("akka", "snapshots"),
      fork in run := true,
      fork in Test := true,
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(Dependencies._scalatest % Test))
}

object Publishing {
  lazy val publishing = Seq(
    publishTo := (if (version.value.endsWith("-SNAPSHOT")) {
                    Some("Helloscala_sbt-public_snapshot".at(
                      "https://artifactory.hongkazhijia.com/artifactory/sbt-release;build.timestamp=" + new java.util.Date().getTime))
                  } else {
                    Some(
                      "Helloscala_sbt-public_release".at(
                        "https://artifactory.hongkazhijia.com/artifactory/libs-release"))
                  }),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials_akka-fusion"))

  lazy val noPublish =
    Seq(publish := ((): Unit), publishLocal := ((): Unit), publishTo := None)
}

object Environment {
  object BuildEnv extends Enumeration {
    val Production, Stage, Test, Developement = Value
  }

  val buildEnv = settingKey[BuildEnv.Value]("The current build environment")

  val settings = Seq(onLoadMessage := {
    // old message as well
    val defaultMessage = onLoadMessage.value
    val env = buildEnv.value
    s"""|$defaultMessage
          |Working in build environment: $env""".stripMargin
  })
}

object Packaging {
  // Good example https://github.com/typesafehub/activator/blob/master/project/Packaging.scala
  import Environment.BuildEnv
  import Environment.buildEnv
  import com.typesafe.sbt.SbtNativePackager._
  import com.typesafe.sbt.packager.Keys._
  import sbtassembly.AssemblyKeys.assembly
  import sbtassembly.AssemblyKeys.assemblyMergeStrategy
  import sbtassembly.MergeStrategy
  import sbtassembly.PathList

  // This is dirty, but play has stolen our keys, and we must mimc them here.
  val stage = TaskKey[File]("stage")
  val dist = TaskKey[File]("dist")

  val settings = Seq(
    name in Universal := s"${name.value}",
    dist := (packageBin in Universal).value,
    mappings in Universal += {
      val confFile = buildEnv.value match {
        case BuildEnv.Developement => "dev.conf"
        case BuildEnv.Test         => "test.conf"
        case BuildEnv.Stage        => "stage.conf"
        case BuildEnv.Production   => "prod.conf"
      }
      (sourceDirectory(_ / "universal" / "conf").value / confFile) -> "conf/application.conf"
    },
    bashScriptExtraDefines ++= Seq(
        """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
        """addJava "-Dpidfile.path=${app_home}/../run/%s.pid"""".format(name.value),
        """addJava "-Dlogback.configurationFile=${app_home}/../conf/logback.xml""""),
    bashScriptConfigLocation := Some("${app_home}/../conf/jvmopts"),
    scriptClasspath := Seq("*"),
    mappings in (Compile, packageDoc) := Seq())

  // Create a new MergeStrategy for aop.xml files
  val aopMerge: MergeStrategy = new MergeStrategy {
    val name = "aopMerge"
    import scala.xml._
    import scala.xml.dtd._

    def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
      val dt =
        DocType("aspectj", PublicID("-//AspectJ//DTD//EN", "http://www.eclipse.org/aspectj/dtd/aspectj.dtd"), Nil)
      val file = MergeStrategy.createMergeTarget(tempDir, path)
      val xmls: Seq[Elem] = files.map(XML.loadFile)
      val aspectsChildren: Seq[Node] =
        xmls.flatMap(_ \\ "aspectj" \ "aspects" \ "_")
      val weaverChildren: Seq[Node] =
        xmls.flatMap(_ \\ "aspectj" \ "weaver" \ "_")
      val options: String = xmls.map(x => (x \\ "aspectj" \ "weaver" \ "@options").text).mkString(" ").trim
      val weaverAttr =
        if (options.isEmpty) Null
        else new UnprefixedAttribute("options", options, Null)
      val aspects =
        new Elem(null, "aspects", Null, TopScope, false, aspectsChildren: _*)
      val weaver = new Elem(null, "weaver", weaverAttr, TopScope, false, weaverChildren: _*)
      val aspectj =
        new Elem(null, "aspectj", Null, TopScope, false, aspects, weaver)
      XML.save(file.toString, aspectj, "UTF-8", xmlDecl = false, dt)
      IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
      Right(Seq(file -> path))
    }
  }

  def assemblySettings =
    Seq(test in assembly := {}, assemblyMergeStrategy in assembly := {
      case PathList("javax", "servlet", xs @ _*)                => MergeStrategy.first
      case PathList("io", "netty", xs @ _*)                     => MergeStrategy.first
      case PathList("jnr", xs @ _*)                             => MergeStrategy.first
      case PathList("com", "datastax", xs @ _*)                 => MergeStrategy.first
      case PathList("com", "kenai", xs @ _*)                    => MergeStrategy.first
      case PathList("org", "objectweb", xs @ _*)                => MergeStrategy.first
      case PathList(ps @ _*) if ps.last.endsWith(".html")       => MergeStrategy.first
      case PathList("org", "slf4j", xs @ _*)                    => MergeStrategy.first
      case PathList("google", "protobuf", xs @ _*)              => MergeStrategy.first
      case PathList("com", "google", "protobuf", xs @ _*)       => MergeStrategy.first
      case "application.conf"                                   => MergeStrategy.concat
      case "reference.conf"                                     => MergeStrategy.concat
      case "module-info.class"                                  => MergeStrategy.concat
      case "META-INF/io.netty.versions.properties"              => MergeStrategy.first
      case "META-INF/native/libnetty-transport-native-epoll.so" => MergeStrategy.first
      case n if n.endsWith(".txt")                              => MergeStrategy.concat
      case n if n.endsWith("NOTICE")                            => MergeStrategy.concat
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    })
}
