val kryoVersion = "5.6.2"
val asmVersion = "4.29"

def scalaVersionSpecificFolders(srcBaseDir: java.io.File, scalaVersion: String): List[File] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, y)) if y >= 13 =>
      new java.io.File(s"${srcBaseDir.getPath}-2.13+") :: Nil
    case _ => Nil
  }

val sharedSettings = Seq(
  organization := "pl.touk",
  organizationName := "TouK",
  organizationHomepage := Some(url("https://touk.pl/")),
  licenses := List(License.Apache2),
  homepage := Some(url("https://github.com/TouK/chill")),
  scalaVersion := "2.13.18",
  crossScalaVersions := Seq("2.13.18"),
  scalacOptions ++= Seq("-unchecked", "-deprecation"),
  scalacOptions ++= Seq(
    "-Ywarn-unused",
    "-release",
    "11",
    "-Wconf:msg=symbol literal is deprecated:s"
  ),
  javacOptions ++= Seq(
    "--release",
    "11",
    "-Xlint:deprecation"
  ), // without '-Xlint:unchecked', original codebase
  Test / fork := true,
  Test / javaOptions ++= Seq(
    "--add-opens",
    "java.base/java.util=ALL-UNNAMED",
    "--add-opens",
    "java.base/java.lang.invoke=ALL-UNNAMED"
  ),
  doc / javacOptions := Seq("--release", "11"),
  resolvers ++= Seq(
    Resolver.sonatypeCentralSnapshots
  ),
  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck" % "1.19.0" % "test",
    "org.scalatest" %% "scalatest" % "3.2.19" % "test",
    "org.scalatestplus" %% "scalacheck-1-19" % "3.2.19.0" % "test",
    "com.esotericsoftware" % "kryo" % kryoVersion
  ),
  Test / parallelExecution := true,
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
  .aggregate(
    chillScala,
    chillJava
  )

lazy val noPublishSettings = Seq(
  publish / skip := true,
  publish := {},
  publishLocal := {},
  test := {},
  publishArtifact := false
)

lazy val chillScala = Project(id = "chill", base = file("chill-scala"))
  .settings(sharedSettings)
  .settings(
    name := "chill",
    libraryDependencies += "org.apache.xbean" % "xbean-asm9-shaded" % asmVersion
  )
  .dependsOn(chillJava)

// This can only have java deps!
lazy val chillJava = Project(id = "chill-java", base = file("chill-java"))
  .settings(sharedSettings)
  .settings(
    name := "chill-java",
    publishArtifact := true
  )
  .settings(
    crossPaths := false,
    autoScalaLibrary := false
  )

inThisBuild(
  List(
    scalaVersion := "2.13.18",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

inThisBuild(
  Seq(
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      if ((chillAll / isSnapshot).value)
        Some("central-snapshots".at("https://central.sonatype.com/repository/maven-snapshots/"))
      else localStaging.value
    },
    Test / publishArtifact := false,
    scmInfo := Some(ScmInfo(url("https://github.com/TouK/chill"), "scm:git@github.com:TouK/chill.git")),
    developers := List(
      Developer(id = "TouK", name = "TouK", email = "", url = url("https://touk.pl"))
    )
  )
)
