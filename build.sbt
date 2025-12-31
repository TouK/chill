val akkaVersion = "2.6.20"
val algebirdVersion = "0.13.9"
val bijectionVersion = "0.9.7"
val kryoVersion = "5.6.2"
val scroogeVersion = "21.2.0"
val asmVersion = "4.16"
val protobufVersion = "3.22.2"

def scalaVersionSpecificFolders(srcBaseDir: java.io.File, scalaVersion: String): List[File] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, y)) if y <= 12 =>
      new java.io.File(s"${srcBaseDir.getPath}-2.12-") :: Nil
    case Some((2, y)) if y >= 13 =>
      new java.io.File(s"${srcBaseDir.getPath}-2.13+") :: Nil
    case _ => Nil
  }

val sharedSettings = Seq(
  organization := "com.xebialabs.chill",
  scalaVersion := "2.13.13",
  crossScalaVersions := Seq("2.11.12", "2.12.17", "2.13.13"),
  scalacOptions ++= Seq("-unchecked", "-deprecation"),
  scalacOptions ++= {
    val credentialsFile = Path.userHome / ".sbt" / ".chill-credentials"
    scalaVersion.value match {
      case v if v.startsWith("2.11") => Seq("-Ywarn-unused", "-Ywarn-unused-import", "-target:jvm-1.8")
      case _                         => Seq("-Ywarn-unused", "-release", "8")
    }
  },
  // Twitter Hadoop needs this, sorry 1.7 fans
  javacOptions ++= Seq("-target", "1.8", "-source", "1.8", "-Xlint:-options"),
  Test / fork := true,
  Test / javaOptions ++= {
    sys.props("java.version") match {
      case v if v.startsWith("17") =>
        Seq(
          "--add-opens",
          "java.base/java.util=ALL-UNNAMED",
          "--add-opens",
          "java.base/java.lang.invoke=ALL-UNNAMED"
        )
      case _ => Seq.empty
    }
  },
  doc / javacOptions := Seq("-source", "1.8"),
  resolvers ++= Seq(
    Opts.resolver.sonatypeSnapshots,
    Opts.resolver.sonatypeReleases,
    "clojars".at("https://clojars.org/repo")
  ),
  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck" % "1.15.2" % "test",
    "org.scalatest" %% "scalatest" % "3.2.15" % "test",
    "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % "test",
    "com.esotericsoftware" % "kryo" % kryoVersion
  ),
  Test / parallelExecution := true,
  pomExtra := <url>https://github.com/xebialabs/chill</url>
        <licenses>
      <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      <comments>A business-friendly OSS license</comments>
      </license>
      </licenses>
      <developers>
      <developer>
      <id>oscar</id>
      <name>Oscar Boykin</name>
      <url>http://twitter.com/posco</url>
        </developer>
      <developer>
      <id>sritchie</id>
      <name>Sam Ritchie</name>
      <url>http://twitter.com/sritchie</url>
        </developer>
      </developers>,
  Compile / unmanagedSourceDirectories ++= scalaVersionSpecificFolders(
    (Compile / scalaSource).value,
    scalaVersion.value
  ),
  Test / unmanagedSourceDirectories ++= scalaVersionSpecificFolders(
    (Test / scalaSource).value,
    scalaVersion.value
  ),
  Compile / unmanagedSourceDirectories ++= scalaVersionSpecificFolders(
    (Compile / javaSource).value,
    scalaVersion.value
  )
)

// Aggregated project
lazy val chillAll = Project(
  id = "chill-all",
  base = file(".")
).settings(sharedSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := Nil
  )
  .aggregate(
    chill,
    chillBijection,
    chillScrooge,
    chillJava,
    chillHadoop,
    chillThrift,
    chillProtobuf,
    chillAkka,
    chillAvro,
    chillAlgebird
  )

lazy val noPublishSettings = Seq(
  publish / skip := true,
  publish := {},
  publishLocal := {},
  test := {},
  publishArtifact := false
)

val unreleasedModules = Set[String]("akka")
val javaOnly = Set[String]("java", "hadoop", "thrift", "protobuf")

def module(name: String) = {
  val id = "chill-%s".format(name)
  Project(id = id, base = file(id))
    .settings(sharedSettings)
    .settings(
      Keys.name := id,
      // Disable cross publishing for java artifacts
      publishArtifact :=
        (if (javaOnly.contains(name) && scalaVersion.value.startsWith("2.11")) false else true)
    )
}

// We usually do the pattern of having a core module, but we don't want to cause
// pain for legacy deploys. With this, they can stay the same.
lazy val chill = Project(
  id = "chill",
  base = file("chill-scala")
).settings(sharedSettings)
  .settings(
    name := "chill",
    libraryDependencies += "org.apache.xbean" % "xbean-asm7-shaded" % asmVersion
  )
  .dependsOn(chillJava)

def akka(scalaVersion: String) =
  (scalaVersion match {
    case s if s.startsWith("2.11.") => "com.typesafe.akka" %% "akka-actor" % "2.5.32"
    case _                          => "com.typesafe.akka" %% "akka-actor" % akkaVersion
  }) % "provided"

lazy val chillAkka = module("akka")
  .settings(
    resolvers += Resolver.typesafeRepo("releases"),
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.2",
      scalaVersion(sv => akka(sv)).value
    )
  )
  .dependsOn(chill % "test->test;compile->compile")

lazy val chillBijection = module("bijection")
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "bijection-core" % bijectionVersion
    )
  )
  .dependsOn(chill % "test->test;compile->compile")

// This can only have java deps!
lazy val chillJava = module("java").settings(
  crossPaths := false,
  autoScalaLibrary := false
)

// This can only have java deps!
lazy val chillHadoop = module("hadoop")
  .settings(
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      "org.apache.hadoop" % "hadoop-core" % "1.2.1" % "provided",
      "org.slf4j" % "slf4j-api" % "2.0.7",
      "org.slf4j" % "slf4j-log4j12" % "2.0.7" % "provided"
    )
  )
  .dependsOn(chillJava)

// This can only have java deps!
lazy val chillThrift = module("thrift").settings(
  crossPaths := false,
  autoScalaLibrary := false,
  libraryDependencies ++= Seq(
    "org.apache.thrift" % "libthrift" % "0.17.0" % "provided"
  )
)

lazy val chillScrooge = module("scrooge")
  .settings(
    libraryDependencies ++= Seq(
      ("org.apache.thrift" % "libthrift" % "0.17.0").exclude("junit", "junit"),
      "com.twitter" %% "scrooge-serializer" % scroogeVersion
    )
  )
  .dependsOn(chill % "test->test;compile->compile")

// This can only have java deps!
lazy val chillProtobuf = module("protobuf")
  .settings(
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % protobufVersion % Provided,
    Test / PB.targets := Seq(
      PB.gens.java(protobufVersion) -> (Test / sourceManaged).value
    )
  )
  .dependsOn(chillJava)

lazy val chillAvro = module("avro")
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "bijection-avro" % bijectionVersion,
      "junit" % "junit" % "4.13.2" % "test"
    )
  )
  .dependsOn(chill, chillJava, chillBijection)

lazy val chillAlgebird = module("algebird")
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "algebird-core" % algebirdVersion
    )
  )
  .dependsOn(chill)

ThisBuild / dynverSeparator := "-"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / publishMavenStyle := true

ThisBuild / publishTo := {
  val nexus = "https://nexus.xebialabs.com/nexus/content/"
  if (isSnapshot.value)
    Some("snapshots".at(nexus + "repositories/snapshots"))
  else
    Some("releases".at(nexus + "repositories/releases"))
}

inThisBuild(
  List(
    scalaVersion := "2.13.13",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)
