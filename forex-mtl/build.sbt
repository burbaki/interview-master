import Dependencies._

name := "forex"
version := "1.0.1"

scalaVersion := "2.12.10"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Xlint",
  "-Ydelambdafy:method",
  "-Xlog-reflective-calls",
  "-Yno-adapted-args",
  "-Ypartial-unification",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  compilerPlugin(Libraries.kindProjector),
  Libraries.cats,
  Libraries.catsEffect,
  Libraries.fs2,
  Libraries.http4sDsl,
  Libraries.http4sServer,
  Libraries.http4sClient,
  Libraries.http4sCirce,
  Libraries.circeCore,
  Libraries.circeGeneric,
  Libraries.circeGenericExt,
  Libraries.circeParser,
  Libraries.circeJava8,
  Libraries.pureConfig,
  Libraries.logback,
  Libraries.log4cats,
  Libraries.log4catsSlf4j,
  Libraries.scalaTest      % Test,
  Libraries.scalaCheck     % Test,
  Libraries.catsScalaCheck % Test,
  Libraries.scalaMockito   % Test
)
