val mainScala             = "2.12.11"
val allScala              = Seq("2.13.2", mainScala)
val akkaVersion           = "2.6.8"
val catsEffectVersion     = "2.1.4"
val circeVersion          = "0.13.0"
val http4sVersion         = "0.21.6"
val playVersion           = "2.8.1"
val silencerVersion       = "1.6.0"
val sttpVersion           = "2.2.3"
val tapirVersion          = "0.16.10"
val zioVersion            = "1.0.0-RC21-2"
val zioInteropCatsVersion = "2.1.4.0-RC17"
val zioConfigVersion      = "1.0.0-RC25"
val zqueryVersion         = "0.2.3"

lazy val root = project
  .in(file("."))
  .settings(name := "caliban-copy")
  .settings(historyPath := None)
  .aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(name := "caliban")
  .settings(commonSettings)
  .settings(
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    libraryDependencies ++= Seq(
      "com.lihaoyi"       %% "fastparse"    % "2.3.3",
      "com.propensive"    %% "magnolia"     % "0.17.0",
      "com.propensive"    %% "mercator"     % "0.2.1",
      "dev.zio"           %% "zio"          % zioVersion,
      "dev.zio"           %% "zio-streams"  % zioVersion,
      "dev.zio"           %% "zio-query"    % zqueryVersion,
      "dev.zio"           %% "zio-test"     % zioVersion % "test",
      "dev.zio"           %% "zio-test-sbt" % zioVersion % "test",
      "io.circe"          %% "circe-core"   % circeVersion % Optional,
      "com.typesafe.play" %% "play-json"    % playVersion % Optional,
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )
  )
  .settings(
    fork in Test := true,
    fork in run := true
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias(
  "check",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)

val commonSettings = Def.settings(
  scalaVersion := mainScala,
  crossScalaVersions := allScala,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-explaintypes",
    "-Yrangepos",
    "-feature",
    "-language:higherKinds",
    "-language:existentials",
    "-unchecked",
    "-Xlint:_,-type-parameter-shadow",
    "-Xfatal-warnings",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused:patvars,-implicits",
    "-Ywarn-value-discard"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) =>
      Seq(
        "-Xsource:2.13",
        "-Yno-adapted-args",
        "-Ypartial-unification",
        "-Ywarn-extra-implicit",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-opt-inline-from:<source>",
        "-opt-warnings",
        "-opt:l:inline"
      )
    case _ => Nil
  })
)
